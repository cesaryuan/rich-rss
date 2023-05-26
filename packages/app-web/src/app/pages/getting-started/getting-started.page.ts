import { Component, OnInit, ViewChild } from '@angular/core';
import { WizardService } from '../../services/wizard.service';
import { GqlPuppeteerWaitUntil } from '../../../generated/graphql';
import { PageHeaderComponent } from '../../components/page-header/page-header.component';

export const isUrl = (value: string): boolean => {
  if (!value || value.length < 3) {
    return false;
  }
  const potentialUrl = value.toLowerCase();
  if (
    potentialUrl.startsWith('http://') ||
    potentialUrl.startsWith('https://')
  ) {
    try {
      new URL(value);

      const urlPattern =
        /[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{2,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)?/gi;
      return !!potentialUrl.match(new RegExp(urlPattern));
    } catch (e) {
      return false;
    }
  } else {
    return isUrl(`https://${potentialUrl}`);
  }
};

export const fixUrl = (value: string): string => {
  const potentialUrl = value.trim();
  if (
    potentialUrl.toLowerCase().startsWith('http://') ||
    potentialUrl.toLowerCase().startsWith('https://')
  ) {
    return potentialUrl;
  } else {
    return `https://${potentialUrl}`;
  }
};

@Component({
  selector: 'app-getting-started',
  templateUrl: './getting-started.page.html',
  styleUrls: ['./getting-started.page.scss'],
})
export class GettingStartedPage implements OnInit {
  @ViewChild('headerComponent')
  headerComponent: PageHeaderComponent;

  constructor(private readonly wizardService: WizardService) {}

  ngOnInit() {}

  async openWizard(url: string) {
    if (isUrl(url)) {
      await this.wizardService.openFeedWizard({
        fetchOptions: {
          websiteUrl: fixUrl(url),
          prerender: false,
          prerenderWaitUntil: GqlPuppeteerWaitUntil.Load,
          prerenderScript: '',
        },
      });
      this.headerComponent.refresh();
    }
  }
}
