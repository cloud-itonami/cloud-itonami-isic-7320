import test from 'node:test';import assert from 'node:assert/strict';import {calculate} from './calculate.mjs';
const costs={inference:100000,retrieval:100000,verification:100000,storage:100000,settlement:100000,infrastructure:50000,rework:50000};
test('unknown costs cannot become free inputs',()=>assert.equal(calculate({}).status,'needs-cost-measurement'));
test('margin floor preserves 70 percent at rounding boundary',()=>{const r=calculate({costStatus:'measured',costMicroUsdc:{...costs,rework:50001}});assert.equal(r.priceMicroUsdc,'2050000');assert.ok(r.marginBps>=7000);assert.equal(r.enabled,false);});
test('negative cost rejected',()=>assert.equal(calculate({costStatus:'measured',costMicroUsdc:{...costs,inference:-1}}).status,'needs-cost-measurement'));
