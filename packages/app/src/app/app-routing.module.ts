import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'search',
    pathMatch: 'full',
  },
  {
    path: 'search',
    loadChildren: () =>
      import('./components/search/search.module').then(
        (m) => m.SearchPageModule
      ),
  },
  {
    path: 'bucket/:id',
    loadChildren: () =>
      import('./components/bucket/bucket.module').then(
        (m) => m.BucketPageModule
      ),
  },
  {
    path: 'bucket/new',
    loadChildren: () => import('./components/bucket-create/bucket-create.module').then( m => m.BucketCreatePageModule)
  },
  {
    path: 'bucket/:id/edit',
    loadChildren: () => import('./components/bucket-edit/bucket-edit.module').then( m => m.BucketEditPageModule)
  },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
