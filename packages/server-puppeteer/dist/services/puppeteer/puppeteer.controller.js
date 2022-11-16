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
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var PuppeteerController_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.PuppeteerController = void 0;
const common_1 = require("@nestjs/common");
const puppeteer_service_1 = require("./puppeteer.service");
const corrId_1 = require("../../libs/corrId");
let PuppeteerController = PuppeteerController_1 = class PuppeteerController {
    constructor(puppeteer) {
        this.puppeteer = puppeteer;
        this.logger = new common_1.Logger(PuppeteerController_1.name);
    }
    async prerenderWebsite(url, corrIdParam, timeoutParam, beforeScript, optimizeParam) {
        const corrId = corrIdParam || (0, corrId_1.newCorrId)();
        const timeoutMillis = this.puppeteer.handleTimeoutParam(timeoutParam);
        const optimize = optimizeParam ? optimizeParam === 'true' : true;
        this.logger.log(`[${corrId}] prerenderWebsite ${url} optimize=${optimize} to=${timeoutParam} -> ${timeoutMillis} script=${beforeScript}`);
        const job = {
            corrId,
            url,
            beforeScript,
            optimize,
            timeoutMillis,
        };
        return this.puppeteer.submit(job);
    }
};
__decorate([
    (0, common_1.Get)('api/intern/prerender'),
    __param(0, (0, common_1.Query)('url')),
    __param(1, (0, common_1.Query)('corrId')),
    __param(2, (0, common_1.Query)('timeout')),
    __param(3, (0, common_1.Query)('script')),
    __param(4, (0, common_1.Query)('optimize')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String, String, String]),
    __metadata("design:returntype", Promise)
], PuppeteerController.prototype, "prerenderWebsite", null);
PuppeteerController = PuppeteerController_1 = __decorate([
    (0, common_1.Controller)(),
    __metadata("design:paramtypes", [puppeteer_service_1.PuppeteerService])
], PuppeteerController);
exports.PuppeteerController = PuppeteerController;
//# sourceMappingURL=puppeteer.controller.js.map