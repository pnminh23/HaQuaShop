import { DialogService } from '../../core/services/dialog.service';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { API_URL } from '../../core/config/api.config';
import { Address, Profile } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { ToastService } from '../../core/services/toast.service';

/** UC22 – tài khoản cá nhân: thông tin, đổi mật khẩu, sổ địa chỉ, điểm HaQua Xanh. */
@Component({
  selector: 'app-account-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './account-page.html',
  styleUrl: './account-page.scss',
})
export class AccountPage {
  private readonly dialog = inject(DialogService);
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);

  protected readonly profile = signal<Profile | null>(null);
  protected readonly editing = signal<Address | null>(null);

  protected readonly infoForm = this.fb.nonNullable.group({ fullName: ['', Validators.required], email: ['', [Validators.required, Validators.email]] });
  protected readonly passForm = this.fb.nonNullable.group({
    oldPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)]],
  });
  protected readonly addrForm = this.fb.nonNullable.group({
    receiverName: ['', Validators.required],
    phone: ['', [Validators.required, Validators.pattern(/^0\d{9}$/)]],
    province: ['', Validators.required],
    ward: ['', Validators.required],
    detail: ['', Validators.required],
    defaultAddress: [false],
  });

  constructor() {
    inject(SeoService).set('Tài khoản của tôi | HaQua');
    this.reload();
  }

  private reload(): void {
    this.auth.profile().subscribe((p) => {
      this.profile.set(p);
      this.infoForm.patchValue({ fullName: p.fullName, email: p.email });
    });
  }

  protected saveInfo(): void {
    if (this.infoForm.invalid) return;
    this.http.put<Profile>(`${API_URL}/account`, this.infoForm.getRawValue()).subscribe({
      next: (p) => { this.profile.set(p); this.toast.show('Đã lưu thông tin'); },
      error: (e) => this.toast.show(e.error?.detail ?? 'Không lưu được'),
    });
  }

  protected changePassword(): void {
    if (this.passForm.invalid) return;
    this.http.put(`${API_URL}/account/password`, this.passForm.getRawValue()).subscribe({
      next: () => { this.toast.show('Đã đổi mật khẩu'); this.passForm.reset(); },
      error: (e) => this.toast.show(e.error?.detail ?? 'Không đổi được mật khẩu'),
    });
  }

  protected editAddress(a: Address | null): void {
    this.editing.set(a ?? { receiverName: '', phone: '', province: '', ward: '', detail: '', defaultAddress: false });
    this.addrForm.reset({ receiverName: a?.receiverName ?? '', phone: a?.phone ?? '', province: a?.province ?? '', ward: a?.ward ?? '', detail: a?.detail ?? '', defaultAddress: a?.defaultAddress ?? false });
  }

  protected saveAddress(): void {
    if (this.addrForm.invalid) return;
    const id = this.editing()?.id;
    const body = this.addrForm.getRawValue();
    const req = id ? this.http.put(`${API_URL}/account/addresses/${id}`, body) : this.http.post(`${API_URL}/account/addresses`, body);
    req.subscribe({ next: () => { this.editing.set(null); this.reload(); }, error: (e) => this.toast.show(e.error?.detail ?? 'Không lưu được địa chỉ') });
  }

  protected async deleteAddress(a: Address): Promise<void> {
    if (a.id && await this.dialog.confirm({ message: 'Xóa địa chỉ này?', confirmLabel: 'Xóa', danger: true })) this.http.delete(`${API_URL}/account/addresses/${a.id}`).subscribe(() => this.reload());
  }
}
