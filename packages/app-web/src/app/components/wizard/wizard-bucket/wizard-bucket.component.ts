import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { WizardStepId } from '../wizard/wizard.component';
import { BucketService } from '../../../services/bucket.service';
import { WizardHandler } from '../wizard-handler';
import { BucketFormData } from '../../bucket-edit/bucket-edit.component';
import { GqlVisibility } from '../../../../generated/graphql';
import { isUndefined } from 'lodash-es';
import { BasicBucket, BucketData, Pagination } from '../../../graphql/types';

@Component({
  selector: 'app-wizard-bucket',
  templateUrl: './wizard-bucket.component.html',
  styleUrls: ['./wizard-bucket.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WizardBucketComponent implements OnInit {
  @Input()
  handler: WizardHandler;

  @Output()
  navigateTo: EventEmitter<WizardStepId> = new EventEmitter<WizardStepId>();
  existingBuckets: Array<BasicBucket> = [];
  modeCreateBucket = true;
  existingBucket: BasicBucket;
  private pagination: Pagination;
  private createBucketData: BucketFormData;

  constructor(
    private readonly bucketService: BucketService,
    private readonly changeRef: ChangeDetectorRef
  ) {}

  async ngOnInit() {
    await this.handler.updateContext({
      isCurrentStepValid: false,
    });
    await this.searchBuckets();
  }

  async searchBuckets() {
    const { buckets, pagination } = await this.bucketService.search({
      cursor: {
        page: 0,
      },
    });
    this.existingBuckets = buckets;
    await this.handleChange();
    this.pagination = pagination;
    this.changeRef.detectChanges();
  }

  async handleCreateBucket(data: BucketFormData) {
    this.createBucketData = data;
    await this.handleChange();
  }

  getBucketFormData(): BucketData {
    const discovery = this.handler.getDiscovery();

    if (this.handler.getContext()?.bucket?.create) {
      return this.handler.getContext()?.bucket?.create as BucketData;
    } else if (discovery) {
      const { document } = discovery;
      return {
        title: document.title,
        description: document.description,
        websiteUrl: document.url,
        visibility: GqlVisibility.IsPublic,
        tags: [],
        imageUrl: '',
      };
    } else {
      return {
        title: '',
        description: '',
        websiteUrl: '',
        visibility: GqlVisibility.IsPublic,
        tags: [],
        imageUrl: '',
      };
    }
  }

  async toggleCreateMode(event: MouseEvent) {
    this.modeCreateBucket = !this.modeCreateBucket;
    event.stopPropagation();
    await this.handleChange();
  }

  async useExistingBucket(bucket: BasicBucket) {
    this.existingBucket = bucket;
    await this.bubbleExistingBucket();
  }

  private async handleChange() {
    if (this.modeCreateBucket) {
      console.log('updateContext', this.createBucketData);
      const { title, websiteUrl, imageUrl, visibility, description, tags } =
        this.createBucketData.data;
      await this.handler.updateContext({
        bucket: {
          create: {
            title,
            tags,
            imageUrl,
            visibility,
            description,
            websiteUrl,
          },
        },
        isCurrentStepValid: this.createBucketData.valid,
      });
    } else {
      await this.bubbleExistingBucket();
    }
  }

  private async bubbleExistingBucket() {
    const bucket = this.existingBucket;
    if (bucket) {
      // console.log('updateContext', bucket);
      await this.handler.updateContext({
        bucket: {
          connect: {
            id: bucket.id,
          },
        },
        isCurrentStepValid: !isUndefined(bucket),
      });
    } else {
      // console.log('updateContext', {
      //   isCurrentStepValid: false,
      // });
      await this.handler.updateContext({
        isCurrentStepValid: false,
      });
    }
  }
}
