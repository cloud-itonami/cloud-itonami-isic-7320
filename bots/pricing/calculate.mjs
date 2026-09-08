import {readFileSync} from 'node:fs';
export function calculate(input) {
 const keys=['inference','retrieval','verification','storage','settlement','infrastructure','rework'];
 if(input.costStatus!=='measured'||keys.some(k=>!Number.isSafeInteger(input.costMicroUsdc?.[k])||input.costMicroUsdc[k]<0))return {status:'needs-cost-measurement',candidateMicroUsdc:2000000,enabled:false};
 const cost=keys.reduce((s,k)=>s+BigInt(input.costMicroUsdc[k]),0n);
 const floor=(cost*100n+29n)/30n; // 70% contribution target; never round down.
 const rounded=((floor+49999n)/50000n)*50000n;
 const price=rounded>2000000n?rounded:2000000n;
 return {status:'calculated',costMicroUsdc:cost.toString(),priceMicroUsdc:price.toString(),marginBps:price===0n?0:Number((price-cost)*10000n/price),enabled:false,reason:'fulfillment-and-settlement-readiness-are-separate'};
}
if(process.argv[2])console.log(JSON.stringify(calculate(JSON.parse(readFileSync(process.argv[2],'utf8'))),null,2));
