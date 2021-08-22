import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NativeFeedComponent } from './native-feed.component';
import { IonicModule } from '@ionic/angular';
import { ArticleModule } from '../article/article.module';

@NgModule({
  declarations: [NativeFeedComponent],
  exports: [NativeFeedComponent],
  imports: [CommonModule, IonicModule, ArticleModule],
})
export class NativeFeedModule {}