import { PuppeteerResponse, PuppeteerService } from './puppeteer.service';
export interface PuppeteerJob {
    corrId: string;
    url: string;
    beforeScript: string;
    optimize: boolean;
    timeoutMillis: number;
}
export declare class PuppeteerController {
    private readonly puppeteer;
    private readonly logger;
    constructor(puppeteer: PuppeteerService);
    prerenderWebsite(url: string, corrIdParam: string, timeoutParam: string, beforeScript: string, optimizeParam: string): Promise<PuppeteerResponse>;
}
