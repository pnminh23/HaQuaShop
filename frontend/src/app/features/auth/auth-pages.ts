import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { TokenResponse } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';

/** Điều hướng sau khi đăng nhập: khách về trang trước, nhân viên/quản trị vào /admin. */
function goAfterAuth(router: Router, route: ActivatedRoute, auth: AuthService): void {
  const user = auth.user();
  if (!user) return;
  const back = route.snapshot.queryParamMap.get('returnUrl');
  void router.navigateByUrl(user.role === 'CUSTOMER' ? (back ?? '/') : auth.homeFor(user));
}

@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.html',
  styleUrl: './auth.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly error = signal('');
  protected readonly busy = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  constructor() {
    inject(SeoService).set('Đăng nhập | HaQua');
  }

  protected submit(): void {
    if (this.form.invalid) return;
    this.busy.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: () => goAfterAuth(this.router, this.route, this.auth),
      error: (e) => {
        this.busy.set(false);
        this.error.set(e.error?.detail ?? 'Không đăng nhập được');
      },
    });
  }
}

@Component({
  selector: 'app-register-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register-page.html',
  styleUrl: './auth.scss',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly error = signal('');
  protected readonly busy = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    fullName: ['', Validators.required],
    phone: ['', [Validators.required, Validators.pattern(/^0\d{9}$/)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)]],
  });

  constructor() {
    inject(SeoService).set('Đăng ký tài khoản | HaQua');
  }

  protected invalid(name: 'fullName' | 'phone' | 'email' | 'password'): boolean {
    const c = this.form.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.busy.set(true);
    (this.auth.register(this.form.getRawValue()) as Observable<TokenResponse>).subscribe({
      next: () => goAfterAuth(this.router, this.route, this.auth),
      error: (e) => {
        this.busy.set(false);
        this.error.set(e.error?.detail ?? 'Không đăng ký được');
      },
    });
  }
}
