import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
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
  DownOutline,
  CameraOutline,
  EnvironmentOutline,
  EditOutline,
  DeleteOutline,
  SaveOutline,
  IdcardOutline,
  SettingOutline,
  SolutionOutline,
  BankOutline,
  FileTextOutline,
  UsergroupAddOutline,
  ArrowLeftOutline,
  SendOutline,
  EyeOutline,
  EyeInvisibleOutline,
  DownloadOutline
} from '@ant-design/icons-angular/icons';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SharedModule } from './shared/shared.module';
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
  DownOutline,
  CameraOutline,
  EnvironmentOutline,
  EditOutline,
  DeleteOutline,
  SaveOutline,
  IdcardOutline,
  SettingOutline,
  SolutionOutline,
  BankOutline,
  FileTextOutline,
  UsergroupAddOutline,
  ArrowLeftOutline,
  SendOutline,
  EyeOutline,
  EyeInvisibleOutline,
  DownloadOutline
];

@NgModule({
  declarations: [AppComponent],
  imports: [BrowserModule, BrowserAnimationsModule, AppRoutingModule, SharedModule],
  providers: [
    provideHttpClient(withInterceptors([authInterceptor])),
    provideNzI18n(fr_FR),
    { provide: NZ_ICONS, useValue: icons }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
