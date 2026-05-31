import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [RouterModule],
  template: `<p>user-list works!</p>`,
})
export class UserListComponent {}
