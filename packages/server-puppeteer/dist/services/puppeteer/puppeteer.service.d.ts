/// <reference types="node" />
import { PuppeteerJob } from './puppeteer.controller';
export interface PuppeteerResponse {
    screenshot?: String | Buffer;
    html?: String;
    isError: Boolean;
    errorMessage?: String;
}
export declare class PuppeteerService {
    private readonly logger;
    private readonly isDebug;
    private readonly queue;
    private readonly maxWorkers;
    private currentActiveWorkers;
    private readonly minTimeout;
    private readonly maxTimeout;
    constructor();
    private static launchLocal;
    submit(job: PuppeteerJob): Promise<PuppeteerResponse>;
    handleTimeoutParam(timeoutParam: string): number;
    private newBrowser;
    private request;
    private newPage;
    private startWorker;
}
