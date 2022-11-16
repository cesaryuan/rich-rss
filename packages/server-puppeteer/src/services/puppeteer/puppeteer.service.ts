import { Browser, Page } from 'puppeteer';
import puppeteer from 'puppeteer-extra';
import { Injectable, Logger } from '@nestjs/common';
import { PuppeteerJob } from './puppeteer.controller';

export interface PuppeteerResponse {
  screenshot?: String | Buffer;
  html?: String;
  isError: Boolean;
  errorMessage?: String;
}

async function grab(page: Page, optimize: boolean) {
  const html = await page.evaluate(() => {
    return document.documentElement.outerHTML;
  });

  if (optimize) {
    return { html, screenshot: null };
  }

  const screenshot = await page.screenshot({ encoding: 'base64', type: 'png' });

  return { html, screenshot };
}

// todo use blocklist to speed up https://github.com/jmdugan/blocklists/tree/master/corporations
@Injectable()
export class PuppeteerService {
  private readonly logger = new Logger(PuppeteerService.name);
  private readonly isDebug: boolean;
  private readonly queue: {
    job: PuppeteerJob;
    queuedAt: number;
    resolve: (response: PuppeteerResponse) => void;
    reject: (reason: string) => void;
  }[] = [];
  private readonly maxWorkers = process.env.MAX_WORKERS || 5;
  private currentActiveWorkers = 0;

  private readonly minTimeout: number =
    parseInt(process.env.MIN_REQ_TIMEOUT_MILLIS, 10) || 150000;
  private readonly maxTimeout: number =
    parseInt(process.env.MAX_REQ_TIMEOUT_MILLIS, 10) || 200000;

  constructor() {
    this.isDebug =
      process.env.DEBUG === 'true' && process.env.NODE_ENV != 'prod';
    this.logger.log(`maxWorkers=${this.maxWorkers}`);
  }

  private static launchLocal(debug: boolean) {
    return puppeteer.launch({
      headless: !debug,
      // devtools: false,
      defaultViewport: {
        width: 1024,
        height: 768,
      },
      executablePath: '/usr/bin/chromium-browser',
      timeout: 10000,
      dumpio: debug,
      args: [
        '--disable-dev-shm-usage',
        // '--disable-background-networking',
        // Disable installation of default apps on first run
        '--disable-default-apps',
        // Disable all chrome extensions entirely
        '--disable-extensions',
        // Disable the GPU hardware acceleration
        '--disable-gpu',
        // Disable syncing to a Google account
        '--disable-sync',
        // Disable built-in Google Translate service
        '--disable-translate',
        // Hide scrollbars on generated images/PDFs
        // '--hide-scrollbars',
        // Disable reporting to UMA, but allows for collection
        // '--metrics-recording-only',
        // Mute audio
        '--mute-audio',
        // Skip first run wizards
        '--no-first-run',
        // Disable sandbox mode
        // '--no-sandbox',
        // Expose port 9222 for remote debugging
        //  '--remote-debugging-port=9222',
        // Disable fetching safebrowsing lists, likely redundant due to disable-background-networking
        '--safebrowsing-disable-auto-update',
      ],
    });
  }

  public async submit(job: PuppeteerJob): Promise<PuppeteerResponse> {
    return new Promise<PuppeteerResponse>((resolve, reject) => {
      this.queue.push({ job, resolve, reject, queuedAt: Date.now() });
      if (this.currentActiveWorkers < this.maxWorkers) {
        this.startWorker(this.currentActiveWorkers).catch(reject);
      }
    }).catch((e) => {
      this.logger.error(e);
      return {
        errorMessage: e?.message,
        screenshot: null,
        isError: true,
        html: null,
      };
    });
  }

  handleTimeoutParam(timeoutParam: string): number {
    try {
      const to = parseInt(timeoutParam, 10);
      if (isNumber(to)) {
        return Math.min(Math.max(to, this.minTimeout), this.maxTimeout);
      }
    } catch (e) {
      // ignore
    }
    return this.minTimeout;
  }

  private async newBrowser(): Promise<Browser> {
    return PuppeteerService.launchLocal(this.isDebug);
  }

  // http://localhost:3000/api/intern/prerender?url=https://derstandard.at

  private async request(
    { corrId, url, beforeScript, optimize, timeoutMillis }: PuppeteerJob,
    browser: Browser,
  ): Promise<PuppeteerResponse> {
    const page = await this.newPage(browser, optimize);
    try {
      await page.goto(url, {
        waitUntil: 'domcontentloaded',
        timeout: timeoutMillis,
      });

      // todo mag support this
      // if (beforeScript) {
      //   this.logger.debug(`[${corrId}] passing beforeScript ${beforeScript}`)
      //   await page.evaluate(beforeScript);
      //   this.logger.debug(`[${corrId}] done`)
      // }

      const { html, screenshot } = await grab(page, optimize);
      return { screenshot, isError: false, html };
    } catch (e) {
      this.logger.log(`[${corrId}] ${e.message}`);
      const { html, screenshot } = await grab(page, optimize);
      return { errorMessage: e.message, screenshot, isError: true, html };
    }
  }

  private async newPage(browser: Browser, optimize: boolean) {
    const page = await browser.newPage();
    await page.setCacheEnabled(false);
    await page.setBypassCSP(true);
    // if (process.env.USER_AGENT) {
    //   await page.setUserAgent(process.env.USER_AGENT);
    // }

    if (optimize) {
      await page.setRequestInterception(true);
      page.on('request', (req: any) => {
        if (
          req.resourceType() == 'stylesheet' ||
          req.resourceType() == 'font' ||
          req.resourceType() == 'image'
        ) {
          req.abort();
        } else {
          req.continue();
        }
      });
    }
    return page;
  }

  private async startWorker(workerId: number) {
    this.logger.debug(`startWorker #${workerId}`);
    this.currentActiveWorkers++;
    while (this.queue.length > 0) {
      const { job, queuedAt, resolve, reject } = this.queue.shift();
      this.logger.debug(
        `worker #${workerId} consumes [${job.corrId}] ${job.url}`,
      );

      const browser = await this.newBrowser();
      try {
        const response = await Promise.race([
          this.request(job, browser),
          new Promise<PuppeteerResponse>((_, reject) =>
            setTimeout(
              () => reject(`timeout ${job.timeoutMillis} exceeded`),
              job.timeoutMillis - 1000,
            ),
          ),
        ]);
        await browser.close();
        this.logger.log(
          `[${job.corrId}] prerendered within ${
            (Date.now() - queuedAt) / 1000
          }s`,
        );
        resolve(response);
      } catch (e) {
        await browser.close();
        this.logger.warn(
          `[${job.corrId}] prerendered within ${
            (Date.now() - queuedAt) / 1000
          }s ${e.message}`,
        );
        reject(e.message);
      }
    }
    this.currentActiveWorkers--;
    this.logger.debug(`endWorker #${workerId}`);
  }
}

function isNumber(value): boolean {
  return typeof value === 'number' && isFinite(value);
}
