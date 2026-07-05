import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [TranslatePipe],
  template: `<div class="home"><h1>{{ 'nav.home' | translate }} — Dars.ma</h1></div>`,
  styles: [`.home { padding: 32px; }`]
})
export class HomeComponent {}
