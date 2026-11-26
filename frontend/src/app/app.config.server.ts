import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';
import { FetchBackend, HttpBackend, HttpRequest } from '@angular/common/http';

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';

/** SSR không đi qua proxy của dev server: gọi thẳng backend. Làm ở tầng cuối, sau mọi interceptor. */
class ServerFetchBackend extends FetchBackend {
  override handle(req: HttpRequest<unknown>) {
    const url = new URL(req.url, 'http://ssr.local');
    return super.handle(
      url.pathname.startsWith('/api/') ? req.clone({ url: BACKEND_URL + url.pathname + url.search }) : req,
    );
  }
}

const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    { provide: FetchBackend, useClass: ServerFetchBackend },
    { provide: HttpBackend, useExisting: FetchBackend },
  ]
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
