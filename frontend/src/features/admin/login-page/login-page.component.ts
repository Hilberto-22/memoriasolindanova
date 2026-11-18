import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../../../api';
import { adminDestination } from '../../../core/auth/admin-destination';
import { PortalBrand } from '../../../shared/components/portal-brand/portal-brand.component';
import { PortalIcon } from '../../../shared/components/portal-icon/portal-icon.component';

@Component({
  imports:[RouterLink, PortalBrand, PortalIcon, ReactiveFormsModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPage {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly usernameInput = viewChild<ElementRef<HTMLInputElement>>('usernameInput');
  private readonly passwordInput = viewChild<ElementRef<HTMLInputElement>>('passwordInput');
  readonly form = inject(FormBuilder).nonNullable.group({
    username:['',Validators.required], password:['',Validators.required]
  });
  readonly error = signal('');
  readonly busy = signal(false);
  readonly showPassword = signal(false);
  readonly capsLock = signal(false);
  readonly expired = this.route.snapshot.queryParamMap.get('reason') === 'expired';
  readonly destination = adminDestination(this.route.snapshot.queryParamMap.get('returnUrl'));
  submit() {
    if (this.busy()) return;
    this.form.controls.username.setValue(this.form.controls.username.value.trim());
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Informe usuário e senha para continuar.');
      (this.form.controls.username.invalid ? this.usernameInput() : this.passwordInput())?.nativeElement.focus();
      return;
    }
    this.error.set('');
    this.busy.set(true);
    this.api.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.form.controls.password.reset();
        void this.router.navigateByUrl(this.destination, {replaceUrl:true}).finally(() => this.busy.set(false));
      },
      error: (failure: HttpErrorResponse) => {
        this.busy.set(false);
        if (failure.status === 401) {
          this.form.controls.password.reset();
          this.passwordInput()?.nativeElement.focus();
        }
        this.error.set(failure.status === 401 ? 'Usuário ou senha inválidos. Confira os dados e tente novamente.' :
          failure.status === 403 ? 'Não foi possível validar o acesso. Tente entrar novamente.' :
          failure.status === 429 ? 'Muitas tentativas de acesso. Aguarde alguns instantes e tente novamente.' :
          'Não foi possível conectar ao servidor. Tente novamente em instantes.');
      }
    });
  }
}
