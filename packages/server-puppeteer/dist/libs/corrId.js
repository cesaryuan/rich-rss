"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.newCorrId = void 0;
const uuid_1 = require("uuid");
function newCorrId() {
    return (0, uuid_1.v4)().substring(0, 5);
}
exports.newCorrId = newCorrId;
//# sourceMappingURL=corrId.js.map