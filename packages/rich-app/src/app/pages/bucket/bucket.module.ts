import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { BucketPageRoutingModule } from './bucket-routing.module';

import { BucketPage } from './bucket.page';
import { BucketSettingsModule } from '../../components/bucket-settings/bucket-settings.module';
import { ArticleModule } from '../../components/article/article.module';
import { ToolbarModule } from '../../components/toolbar/toolbar.module';
import { BubbleModule } from '../../components/bubble/bubble.module';
import { BaseModule } from '../../components/base/base.module';

@NgModule({
  imports: [
    CommonModule,
    BucketSettingsModule,
    FormsModule,
    IonicModule,
    BucketPageRoutingModule,
    ArticleModule,
    ToolbarModule,
    BubbleModule,
    BaseModule,
  ],
  declarations: [BucketPage],
})
export class BucketPageModule {}
