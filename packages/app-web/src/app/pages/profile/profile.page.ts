import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { OpmlService } from '../../services/opml.service';
import { ProfileService } from '../../services/profile.service';
import { Router } from '@angular/router';
import { Plugin, UserSecret } from '../../graphql/types';
import { ModalController, ToastController } from '@ionic/angular';
import { FormControl } from '@angular/forms';
import { ImportModalComponent } from '../../modals/import-modal/import-modal.component';
import { Subscription } from 'rxjs';

interface PluginAndFc {
  plugin: Plugin;
  fc: FormControl<boolean>;
}

@Component({
  selector: 'app-profile',
  templateUrl: './profile.page.html',
  styleUrls: ['./profile.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage implements OnInit, OnDestroy {
  secrets: UserSecret[] = [];
  plugins: PluginAndFc[];
  private subscriptions: Subscription[] = [];

  constructor(
    private readonly opmlService: OpmlService,
    private readonly changeRef: ChangeDetectorRef,
    private readonly router: Router,
    private readonly toastCtrl: ToastController,
    private readonly modalCtrl: ModalController,
    private readonly profileService: ProfileService
  ) {}

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }

  async importOPML(uploadEvent: Event) {
    await this.opmlService.convertOpmlToJson(uploadEvent);
  }

  async exportOPML() {
    await this.opmlService.exportOPML();
  }

  ngOnInit(): void {
    this.subscriptions.push(
      this.profileService.getProfile().subscribe((profile) => {
        this.secrets.push(...profile.user.secrets);
        this.plugins = profile.user.plugins.map((plugin) => {
          const formControl = new FormControl<boolean>(plugin.value);

          formControl.valueChanges.subscribe((value) =>
            this.updatePluginValue(plugin.id, value)
          );
          return {
            plugin,
            fc: formControl,
          };
        });
        this.changeRef.detectChanges();
      })
    );
  }

  async logout() {
    await this.profileService.logout();
    await this.router.navigateByUrl('/');
  }

  async creteApiToken() {
    const apiToken = await this.profileService.createApiToken();
    console.log(apiToken);
    this.secrets.push(apiToken);
  }

  async deleteSecret(secret: UserSecret) {
    await this.profileService.deleteApiTokens({
      where: {
        in: [secret.id],
      },
    });
  }

  async deleteAccount() {
    await this.profileService.updateCurrentUser({
      purgeScheduledFor: {
        assignNull: false,
      },
    });
    const toast = await this.toastCtrl.create({
      message: 'Account deletion scheduled',
      duration: 3000,
      color: 'success',
    });

    await toast.present();
  }

  async importAny() {
    const modal = await this.modalCtrl.create({
      component: ImportModalComponent,
      showBackdrop: true,
    });
    await modal.present();
  }

  private async updatePluginValue(id: string, value: boolean) {
    await this.profileService.updateCurrentUser({
      plugins: [
        {
          id,
          value: {
            set: value,
          },
        },
      ],
    });
  }
}
