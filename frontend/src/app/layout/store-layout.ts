import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ChatWidget } from '../features/chat/chat-widget';
import { SiteFooter } from './site-footer';
import { SiteHeader } from './site-header';

/** Khung chung của các trang cửa hàng. */
@Component({
  selector: 'app-store-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, SiteHeader, SiteFooter, ChatWidget],
  template: `
    <app-site-header />
    <router-outlet />
    <app-site-footer />
    <app-chat-widget />
  `,
})
export class StoreLayout {}
