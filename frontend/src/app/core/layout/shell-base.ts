import { Directive, OnDestroy, OnInit } from '@angular/core';

/**
 * Shared app-shell behavior: on viewports ≤991px the sider becomes an
 * off-canvas drawer (collapsed = hidden). Desktop keeps normal collapse.
 */
@Directive()
export abstract class ShellBase implements OnInit, OnDestroy {
  isCollapsed = false;
  isMobile = false;

  private static readonly MOBILE_QUERY = '(max-width: 991px)';
  private mediaQuery?: MediaQueryList;
  private mediaListener?: (event: MediaQueryListEvent) => void;

  ngOnInit(): void {
    this.initMobileListener();
  }

  ngOnDestroy(): void {
    if (this.mediaQuery && this.mediaListener) {
      this.mediaQuery.removeEventListener('change', this.mediaListener);
    }
  }

  toggleSider(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  /** Close the drawer after navigating on mobile. */
  closeSiderOnMobile(): void {
    if (this.isMobile) {
      this.isCollapsed = true;
    }
  }

  private initMobileListener(): void {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }
    this.mediaQuery = window.matchMedia(ShellBase.MOBILE_QUERY);
    this.applyViewport(this.mediaQuery.matches);
    this.mediaListener = (event: MediaQueryListEvent) => this.applyViewport(event.matches);
    this.mediaQuery.addEventListener('change', this.mediaListener);
  }

  private applyViewport(mobile: boolean): void {
    const wasMobile = this.isMobile;
    this.isMobile = mobile;
    if (mobile && !wasMobile) {
      this.isCollapsed = true;
    } else if (!mobile && wasMobile) {
      this.isCollapsed = false;
    }
  }
}
