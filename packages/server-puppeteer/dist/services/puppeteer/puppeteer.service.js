"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var PuppeteerService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.PuppeteerService = void 0;
const puppeteer_extra_1 = require("puppeteer-extra");
const common_1 = require("@nestjs/common");
async function grab(page, optimize) {
    const html = await page.evaluate(() => {
        return document.documentElement.outerHTML;
    });
    if (optimize) {
        return { html, screenshot: null };
    }
    const screenshot = await page.screenshot({ encoding: 'base64', type: 'png' });
    return { html, screenshot };
}
let PuppeteerService = PuppeteerService_1 = class PuppeteerService {
    constructor() {
        this.logger = new common_1.Logger(PuppeteerService_1.name);
        this.queue = [];
        this.maxWorkers = process.env.MAX_WORKERS || 5;
        this.currentActiveWorkers = 0;
        this.minTimeout = parseInt(process.env.MIN_REQ_TIMEOUT_MILLIS, 10) || 150000;
        this.maxTimeout = parseInt(process.env.MAX_REQ_TIMEOUT_MILLIS, 10) || 200000;
        this.isDebug =
            process.env.DEBUG === 'true' && process.env.NODE_ENV != 'prod';
        this.logger.log(`maxWorkers=${this.maxWorkers}`);
    }
    static launchLocal(debug) {
        return puppeteer_extra_1.default.launch({
            headless: !debug,
            defaultViewport: {
                width: 1024,
                height: 768,
            },
            executablePath: '/usr/bin/chromium-browser',
            timeout: 10000,
            dumpio: debug,
            args: [
                '--disable-dev-shm-usage',
                '--disable-default-apps',
                '--disable-extensions',
                '--disable-gpu',
                '--disable-sync',
                '--disable-translate',
                '--mute-audio',
                '--no-first-run',
                '--safebrowsing-disable-auto-update',
            ],
        });
    }
    async submit(job) {
        return new Promise((resolve, reject) => {
            this.queue.push({ job, resolve, reject, queuedAt: Date.now() });
            if (this.currentActiveWorkers < this.maxWorkers) {
                this.startWorker(this.currentActiveWorkers).catch(reject);
            }
        }).catch((e) => {
            this.logger.error(e);
            return {
                errorMessage: e === null || e === void 0 ? void 0 : e.message,
                screenshot: null,
                isError: true,
                html: null,
            };
        });
    }
    handleTimeoutParam(timeoutParam) {
        try {
            const to = parseInt(timeoutParam, 10);
            if (isNumber(to)) {
                return Math.min(Math.max(to, this.minTimeout), this.maxTimeout);
            }
        }
        catch (e) {
        }
        return this.minTimeout;
    }
    async newBrowser() {
        return PuppeteerService_1.launchLocal(this.isDebug);
    }
    async request({ corrId, url, beforeScript, optimize, timeoutMillis }, browser) {
        const page = await this.newPage(browser, optimize);
        try {
            await page.goto(url, {
                waitUntil: 'domcontentloaded',
                timeout: timeoutMillis,
            });
            const { html, screenshot } = await grab(page, optimize);
            return { screenshot, isError: false, html };
        }
        catch (e) {
            this.logger.log(`[${corrId}] ${e.message}`);
            const { html, screenshot } = await grab(page, optimize);
            return { errorMessage: e.message, screenshot, isError: true, html };
        }
    }
    async newPage(browser, optimize) {
        const page = await browser.newPage();
        await page.setCacheEnabled(false);
        await page.setBypassCSP(true);
        if (optimize) {
            await page.setRequestInterception(true);
            page.on('request', (req) => {
                if (req.resourceType() == 'stylesheet' ||
                    req.resourceType() == 'font' ||
                    req.resourceType() == 'image') {
                    req.abort();
                }
                else {
                    req.continue();
                }
            });
        }
        return page;
    }
    async startWorker(workerId) {
        this.logger.debug(`startWorker #${workerId}`);
        this.currentActiveWorkers++;
        while (this.queue.length > 0) {
            const { job, queuedAt, resolve, reject } = this.queue.shift();
            this.logger.debug(`worker #${workerId} consumes [${job.corrId}] ${job.url}`);
            const browser = await this.newBrowser();
            try {
                const response = await Promise.race([
                    this.request(job, browser),
                    new Promise((_, reject) => setTimeout(() => reject(`timeout ${job.timeoutMillis} exceeded`), job.timeoutMillis - 1000)),
                ]);
                await browser.close();
                this.logger.log(`[${job.corrId}] prerendered within ${(Date.now() - queuedAt) / 1000}s`);
                resolve(response);
            }
            catch (e) {
                await browser.close();
                this.logger.warn(`[${job.corrId}] prerendered within ${(Date.now() - queuedAt) / 1000}s ${e.message}`);
                reject(e.message);
            }
        }
        this.currentActiveWorkers--;
        this.logger.debug(`endWorker #${workerId}`);
    }
};
PuppeteerService = PuppeteerService_1 = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [])
], PuppeteerService);
exports.PuppeteerService = PuppeteerService;
function isNumber(value) {
    return typeof value === 'number' && isFinite(value);
}
//# sourceMappingURL=puppeteer.service.js.map