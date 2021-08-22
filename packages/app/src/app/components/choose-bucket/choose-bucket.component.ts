import { Component, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { BucketService } from '../../services/bucket.service';
import { GqlBucket } from '../../../generated/graphql';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-choose-bucket',
  templateUrl: './choose-bucket.component.html',
  styleUrls: ['./choose-bucket.component.scss'],
})
export class ChooseBucketComponent implements OnInit {
  buckets: GqlBucket[];

  constructor(
    private readonly modalController: ModalController,
    private readonly bucketService: BucketService,
    private readonly toastService: ToastService
  ) {}

  ngOnInit() {
    this.bucketService
      .getBucketsForUser()
      .valueChanges.subscribe(({ data, error }) => {
        if (error) {
          this.toastService.errorFromApollo(error);
        } else {
          this.buckets = data.findFirstUser.buckets;
        }
      });
  }

  dismissModal() {
    return this.modalController.dismiss();
  }

  chooseBucket(bucket: GqlBucket) {
    return this.modalController.dismiss(bucket);
  }

  createBucket() {
    // tood mag
  }
}