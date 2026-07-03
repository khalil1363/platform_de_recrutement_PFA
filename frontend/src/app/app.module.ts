import { NgModule } from '@angular/core';
import { BrowserModule, provideClientHydration } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import fr from '@angular/common/locales/fr';
import { NZ_ICONS } from 'ng-zorro-antd/icon';
import { provideNzI18n, fr_FR } from 'ng-zorro-antd/i18n';
import { IconDefinition } from '@ant-design/icons-angular';
import {
  UserOutline,
  LockOutline,
  MailOutline,
  PhoneOutline,
  MenuFoldOutline,
  MenuUnfoldOutline,
  TeamOutline,
  LogoutOutline,
  PlusOutline,
  DownOutline
} from '@ant-design/icons-angular/icons';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SharedModule } from './shared/shared.module';
import { HomeComponent } from './home/home.component';
import { authInterceptor } from './core/interceptors/auth.interceptor';

registerLocaleData(fr);

const icons: IconDefinition[] = [
  UserOutline,
  LockOutline,
  MailOutline,
  PhoneOutline,
  MenuFoldOutline,
  MenuUnfoldOutline,
  TeamOutline,
  LogoutOutline,
  PlusOutline,
  DownOutline
];

@NgModule({
  declarations: [AppComponent, HomeComponent],
  imports: [BrowserModule, BrowserAnimationsModule, AppRoutingModule, SharedModule],
  providers: [
    provideClientHydration(),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideNzI18n(fr_FR),
    { provide: NZ_ICONS, useValue: icons }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
