import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ArticlesComponent } from './articles.component';
import { IonicModule } from '@ionic/angular';
import { FilterToolbarModule } from '../filter-toolbar/filter-toolbar.module';
import { ArticleRefModule } from '../article-ref/article-ref.module';

@NgModule({
  declarations: [ArticlesComponent],
  exports: [ArticlesComponent],
  imports: [CommonModule, IonicModule, FilterToolbarModule, ArticleRefModule],
})
export class ArticlesModule {}
