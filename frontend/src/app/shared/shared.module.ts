import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzUploadModule } from 'ng-zorro-antd/upload';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { NzStepsModule } from 'ng-zorro-antd/steps';
import { NzCollapseModule } from 'ng-zorro-antd/collapse';
import { NzCalendarModule } from 'ng-zorro-antd/calendar';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzAlertModule } from 'ng-zorro-antd/alert';

const NG_ZORRO_MODULES = [
  NzButtonModule,
  NzCardModule,
  NzFormModule,
  NzInputModule,
  NzSelectModule,
  NzTableModule,
  NzTagModule,
  NzSwitchModule,
  NzModalModule,
  NzLayoutModule,
  NzMenuModule,
  NzIconModule,
  NzAvatarModule,
  NzDropDownModule,
  NzToolTipModule,
  NzDividerModule,
  NzSpinModule,
  NzEmptyModule,
  NzUploadModule,
  NzPopconfirmModule,
  NzDescriptionsModule,
  NzRadioModule,
  NzStepsModule,
  NzCollapseModule,
  NzCalendarModule,
  NzDatePickerModule,
  NzAlertModule
];

@NgModule({
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ...NG_ZORRO_MODULES],
  exports: [CommonModule, FormsModule, ReactiveFormsModule, ...NG_ZORRO_MODULES]
})
export class SharedModule {}
