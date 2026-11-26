import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DialogHost } from './shared/components/dialog-host';
import { ToastHost } from './shared/components/toast-host';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, ToastHost, DialogHost],
  template: `
    <router-outlet />
    <app-toast-host />
    <app-dialog-host />
  `,
})
export class App {}
