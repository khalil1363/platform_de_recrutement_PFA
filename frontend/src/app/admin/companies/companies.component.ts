import { Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NzMessageService } from 'ng-zorro-antd/message';
import { RecruitmentService } from '../../recruitment/services/recruitment.service';
import { Company, Zone } from '../../recruitment/models/recruitment.model';

interface ZoneCompaniesGroup {
  zoneId: string;
  zoneName: string;
  companies: Company[];
}

interface SearchResult {
  displayName: string;
  lat: number;
  lng: number;
}

@Component({
  selector: 'app-companies',
  templateUrl: './companies.component.html',
  styleUrl: './companies.component.css'
})
export class CompaniesComponent implements OnInit, OnDestroy {
  companies: Company[] = [];
  groupedCompanies: ZoneCompaniesGroup[] = [];
  zones: Zone[] = [];
  loading = false;
  modalVisible = false;
  modalLoading = false;
  mapPickerOpen = false;
  companyForm!: FormGroup;

  searchQuery = '';
  searchResults: SearchResult[] = [];
  searching = false;
  locating = false;
  selectedPlaceLabel = '';
  pendingLngLat: { lng: number; lat: number } | null = null;

  // MapLibre is loaded dynamically (canvas renderer — no broken tile CSS)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private map: any = null;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private marker: any = null;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private maplibre: any = null;
  /** MapLibre uses [lng, lat] */
  private readonly defaultCenter: [number, number] = [10.1815, 36.8065];
  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private readonly recruitmentService: RecruitmentService,
    private readonly fb: FormBuilder,
    private readonly message: NzMessageService,
    private readonly zone: NgZone
  ) {}

  ngOnInit(): void {
    this.companyForm = this.fb.group({
      name: ['', Validators.required],
      zoneId: ['', Validators.required],
      address: [''],
      latitude: [null as number | null],
      longitude: [null as number | null],
      googleMapsUrl: ['']
    });
    this.loadData();
  }

  ngOnDestroy(): void {
    this.clearSearchDebounce();
    this.closeMapPicker();
  }

  get hasMapLocation(): boolean {
    const lat = this.companyForm?.get('latitude')?.value;
    const lng = this.companyForm?.get('longitude')?.value;
    return lat != null && lng != null;
  }

  get locationSummary(): string {
    if (!this.hasMapLocation) {
      return '';
    }
    const lat = Number(this.companyForm.get('latitude')!.value).toFixed(6);
    const lng = Number(this.companyForm.get('longitude')!.value).toFixed(6);
    return `${lat}, ${lng}`;
  }

  loadData(): void {
    this.loading = true;
    this.recruitmentService.getCompanies().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.companies = response.data;
          this.groupCompanies();
        }
      },
      error: () => {
        this.loading = false;
        this.message.error('Erreur de chargement des entreprises');
      }
    });
    this.recruitmentService.getZones().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.zones = response.data;
        }
      }
    });
  }

  openModal(): void {
    this.companyForm.reset({
      name: '',
      zoneId: '',
      address: '',
      latitude: null,
      longitude: null,
      googleMapsUrl: ''
    });
    this.modalVisible = true;
  }

  openMapPicker(): void {
    const lat = this.companyForm.get('latitude')?.value;
    const lng = this.companyForm.get('longitude')?.value;
    this.pendingLngLat =
      lat != null && lng != null ? { lng: Number(lng), lat: Number(lat) } : null;
    this.searchQuery = '';
    this.searchResults = [];
    this.selectedPlaceLabel = '';
    this.mapPickerOpen = true;
    requestAnimationFrame(() => {
      requestAnimationFrame(() => void this.initMap());
    });
  }

  closeMapPicker(): void {
    this.clearSearchDebounce();
    this.destroyMap();
    this.mapPickerOpen = false;
    this.searchQuery = '';
    this.searchResults = [];
    this.searching = false;
    this.locating = false;
  }

  onSearchInput(): void {
    this.clearSearchDebounce();
    const q = this.searchQuery.trim();
    if (q.length < 3) {
      this.searchResults = [];
      return;
    }
    this.searchDebounce = setTimeout(() => void this.searchPlaces(q), 400);
  }

  searchNow(): void {
    this.clearSearchDebounce();
    const q = this.searchQuery.trim();
    if (q.length < 2) {
      this.message.warning('Saisissez au moins 2 caractères');
      return;
    }
    void this.searchPlaces(q);
  }

  selectSearchResult(result: SearchResult): void {
    this.searchResults = [];
    this.searchQuery = result.displayName;
    this.selectedPlaceLabel = result.displayName;
    this.goToLocation(result.lng, result.lat, 16);
  }

  useMyLocation(): void {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      this.message.error('La géolocalisation n\'est pas supportée par ce navigateur');
      return;
    }
    this.locating = true;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.locating = false;
        const { latitude, longitude } = pos.coords;
        this.selectedPlaceLabel = 'Ma position actuelle';
        this.goToLocation(longitude, latitude, 16);
        this.message.success('Position actuelle sélectionnée');
        void this.reverseGeocodeLabel(latitude, longitude);
      },
      (err) => {
        this.locating = false;
        if (err.code === err.PERMISSION_DENIED) {
          this.message.error('Autorisez l\'accès à votre position dans le navigateur');
        } else {
          this.message.error('Impossible d\'obtenir votre position');
        }
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 }
    );
  }

  confirmMapLocation(): void {
    if (!this.pendingLngLat) {
      this.message.warning('Sélectionnez une position (carte, recherche ou ma position)');
      return;
    }
    this.setLocation(this.pendingLngLat.lat, this.pendingLngLat.lng, this.selectedPlaceLabel);
    this.closeMapPicker();
    this.message.success('Localisation sélectionnée');
  }

  clearMapLocation(): void {
    this.companyForm.patchValue({
      latitude: null,
      longitude: null,
      googleMapsUrl: ''
    });
  }

  saveCompany(): void {
    if (this.companyForm.invalid) {
      this.companyForm.markAllAsTouched();
      return;
    }
    this.modalLoading = true;
    const value = this.companyForm.value;
    this.recruitmentService.createCompany({
      name: value.name,
      zoneId: value.zoneId,
      address: value.address || undefined,
      latitude: value.latitude,
      longitude: value.longitude,
      googleMapsUrl: value.googleMapsUrl || undefined
    }).subscribe({
      next: (response) => {
        this.modalLoading = false;
        if (response.success) {
          this.message.success('Entreprise créée');
          this.modalVisible = false;
          this.loadData();
        }
      },
      error: (err) => {
        this.modalLoading = false;
        this.message.error(err.error?.message || 'Erreur lors de la création');
      }
    });
  }

  private groupCompanies(): void {
    const grouped = new Map<string, ZoneCompaniesGroup>();
    for (const company of this.companies) {
      if (!grouped.has(company.zoneId)) {
        grouped.set(company.zoneId, {
          zoneId: company.zoneId,
          zoneName: company.zoneName || 'Zone',
          companies: []
        });
      }
      grouped.get(company.zoneId)!.companies.push(company);
    }
    this.groupedCompanies = Array.from(grouped.values()).sort((a, b) => a.zoneName.localeCompare(b.zoneName));
  }

  private async initMap(): Promise<void> {
    this.destroyMap();
    const el = document.getElementById('agency-map');
    if (!el || typeof window === 'undefined') {
      return;
    }

    const mod: any = await import('maplibre-gl');
    this.maplibre = mod.Map ? mod : mod.default;

    const center: [number, number] = this.pendingLngLat
      ? [this.pendingLngLat.lng, this.pendingLngLat.lat]
      : this.defaultCenter;
    const zoom = this.pendingLngLat ? 15 : 12;

    this.map = new this.maplibre.Map({
      container: el,
      style: {
        version: 8,
        sources: {
          'raster-tiles': {
            type: 'raster',
            tiles: [
              'https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
              'https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
              'https://c.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png'
            ],
            tileSize: 256,
            attribution: '&copy; OpenStreetMap &copy; CARTO'
          }
        },
        layers: [
          {
            id: 'simple-tiles',
            type: 'raster',
            source: 'raster-tiles',
            minzoom: 0,
            maxzoom: 20
          }
        ]
      },
      center,
      zoom
    });

    this.map.addControl(new this.maplibre.NavigationControl(), 'top-right');

    this.map.on('load', () => {
      this.ensureSelectionLayers();
      this.map?.resize();
      if (this.pendingLngLat) {
        this.placeMarker(this.pendingLngLat.lng, this.pendingLngLat.lat);
      }
    });

    this.map.on('click', (event: { lngLat: { lng: number; lat: number } }) => {
      this.zone.run(() => {
        this.selectedPlaceLabel = '';
        this.goToLocation(event.lngLat.lng, event.lngLat.lat);
        void this.reverseGeocodeLabel(event.lngLat.lat, event.lngLat.lng);
      });
    });

    setTimeout(() => this.map?.resize(), 100);
    setTimeout(() => this.map?.resize(), 300);
  }

  private goToLocation(lng: number, lat: number, zoom = 15): void {
    this.placeMarker(lng, lat);
    if (this.map) {
      this.map.flyTo({ center: [lng, lat], zoom, essential: true });
    }
  }

  private ensureSelectionLayers(): void {
    if (!this.map || this.map.getSource('selected-point')) {
      return;
    }
    this.map.addSource('selected-point', {
      type: 'geojson',
      data: {
        type: 'FeatureCollection',
        features: []
      }
    });
    this.map.addLayer({
      id: 'selected-point-halo',
      type: 'circle',
      source: 'selected-point',
      paint: {
        'circle-radius': 18,
        'circle-color': '#e53935',
        'circle-opacity': 0.25
      }
    });
    this.map.addLayer({
      id: 'selected-point-dot',
      type: 'circle',
      source: 'selected-point',
      paint: {
        'circle-radius': 8,
        'circle-color': '#e53935',
        'circle-stroke-width': 3,
        'circle-stroke-color': '#ffffff'
      }
    });
  }

  private updateSelectionPoint(lng: number, lat: number): void {
    if (!this.map) {
      return;
    }
    if (!this.map.getSource('selected-point')) {
      // Map not fully loaded yet — retry shortly.
      setTimeout(() => this.updateSelectionPoint(lng, lat), 150);
      return;
    }
    const source = this.map.getSource('selected-point');
    source.setData({
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: { type: 'Point', coordinates: [lng, lat] },
          properties: {}
        }
      ]
    });
  }

  private placeMarker(lng: number, lat: number): void {
    this.pendingLngLat = { lng, lat };
    if (!this.map || !this.maplibre) {
      return;
    }

    this.updateSelectionPoint(lng, lat);

    if (this.marker) {
      this.marker.setLngLat([lng, lat]);
      return;
    }

    // Built-in MapLibre SVG pin (always visible, no custom CSS needed)
    this.marker = new this.maplibre.Marker({
      color: '#e53935',
      scale: 1.35,
      draggable: true,
      anchor: 'bottom'
    })
      .setLngLat([lng, lat])
      .setPopup(
        new this.maplibre.Popup({ offset: 28, closeButton: false }).setText('Position sélectionnée')
      )
      .addTo(this.map);

    this.marker.togglePopup();

    this.marker.on('dragend', () => {
      const pos = this.marker?.getLngLat();
      if (pos) {
        this.zone.run(() => {
          this.pendingLngLat = { lng: pos.lng, lat: pos.lat };
          this.selectedPlaceLabel = '';
          this.updateSelectionPoint(pos.lng, pos.lat);
          void this.reverseGeocodeLabel(pos.lat, pos.lng);
        });
      }
    });
  }

  private async searchPlaces(query: string): Promise<void> {
    this.searching = true;
    try {
      const url =
        `https://nominatim.openstreetmap.org/search?format=jsonv2&q=${encodeURIComponent(query)}`
        + `&limit=6&addressdetails=1&countrycodes=tn`;
      const res = await fetch(url, {
        headers: {
          Accept: 'application/json'
        }
      });
      if (!res.ok) {
        throw new Error('search failed');
      }
      const data = (await res.json()) as Array<{ display_name: string; lat: string; lon: string }>;
      this.searchResults = (data || []).map((item) => ({
        displayName: item.display_name,
        lat: Number(item.lat),
        lng: Number(item.lon)
      }));
      if (this.searchResults.length === 0) {
        this.message.info('Aucun résultat trouvé');
      }
    } catch {
      this.searchResults = [];
      this.message.error('Erreur lors de la recherche');
    } finally {
      this.searching = false;
    }
  }

  private async reverseGeocodeLabel(lat: number, lng: number): Promise<void> {
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`,
        { headers: { Accept: 'application/json' } }
      );
      if (!res.ok) {
        return;
      }
      const data = await res.json();
      if (data?.display_name) {
        this.selectedPlaceLabel = data.display_name as string;
      }
    } catch {
      // ignore
    }
  }

  private setLocation(lat: number, lng: number, placeLabel?: string): void {
    const googleMapsUrl = `https://www.google.com/maps?q=${lat},${lng}`;
    const address = placeLabel || this.companyForm.get('address')?.value || '';
    this.companyForm.patchValue({
      latitude: lat,
      longitude: lng,
      googleMapsUrl,
      ...(address ? { address } : {})
    });
    if (!address) {
      this.fillAddressFromCoords(lat, lng);
    }
  }

  private fillAddressFromCoords(lat: number, lng: number): void {
    fetch(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`, {
      headers: { Accept: 'application/json' }
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        const label = data?.display_name as string | undefined;
        if (label && !this.companyForm.get('address')?.value) {
          this.companyForm.patchValue({ address: label });
        }
      })
      .catch(() => undefined);
  }

  private destroyMap(): void {
    if (this.marker) {
      this.marker.remove();
      this.marker = null;
    }
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
    this.maplibre = null;
  }

  private clearSearchDebounce(): void {
    if (this.searchDebounce) {
      clearTimeout(this.searchDebounce);
      this.searchDebounce = null;
    }
  }
}
