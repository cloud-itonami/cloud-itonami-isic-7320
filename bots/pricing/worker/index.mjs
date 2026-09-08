import {DurableObject} from 'cloudflare:workers';
export class PricingResident extends DurableObject {
 async review(env) {
  const startedAt=new Date().toISOString();
  await this.ctx.storage.put('status',{state:'running',startedAt});
  try {
   const response=await fetch('https://api.murakumo.cloud/v1/chat/completions',{method:'POST',headers:{'content-type':'application/json','user-agent':'Itonami-Pricing-Bot/1.0',authorization:'Bearer '+env.MURAKUMO_API_KEY},body:JSON.stringify({model:'murakumo-main',max_tokens:512,stream:false,messages:[{role:'system',content:'You are the Itonami pricing reviewer. Do not claim tool execution. Return JSON only, without markdown.'},{role:'user',content:'Calculate maximum cost for sale price 2 USDC and contribution margin 70%. Return {"price":2,"margin":0.7,"maxCost":0.6,"costStatus":"unmeasured","demandStatus":"unmeasured"}. Verify arithmetic. No price changes or purchases.'}]}),signal:AbortSignal.timeout(55000),redirect:'manual'});
   if(!response.ok)throw Error('murakumo-http-'+response.status);
   const result=await response.json(),choice=result.choices?.[0];
   if(choice?.finish_reason!=='stop')throw Error('incomplete-model-output');
   const decision=JSON.parse(choice.message.content);
   if(decision.price!==2||decision.margin!==0.7||decision.maxCost!==0.6||decision.costStatus!=='unmeasured'||decision.demandStatus!=='unmeasured')throw Error('invalid-calculation');
   const receipt={state:'completed',startedAt,finishedAt:new Date().toISOString(),provider:'murakumo',model:result.model,requestId:result.id,usage:result.usage,decision,salesEnabled:false,receiver:'0xA00366234D29d4F882088048c0B2fa0dB7302D4E',chain:'eip155:8453'};
   await this.ctx.storage.put('status',receipt);await this.ctx.storage.put('lastSuccess',receipt);
  } catch(e) {await this.ctx.storage.put('status',{state:'failed',startedAt,finishedAt:new Date().toISOString(),error:e.message,salesEnabled:false});}
 }
 async fetch(request) {
  if(new URL(request.url).pathname==='/review') {if(this.running)return new Response('Already running',{status:409});this.running=this.review(this.env);try{await this.running;return Response.json(await this.ctx.storage.get('status'));}finally{this.running=null;}}
  return Response.json({status:await this.ctx.storage.get('status')||{state:'not-run'},lastSuccess:await this.ctx.storage.get('lastSuccess')||null});
 }
}
export default {
 async scheduled(event,env,ctx){ctx.waitUntil(env.PRICING.getByName('pricing').fetch('https://internal/review'));},
 async fetch(request,env){const url=new URL(request.url);if(request.method==='POST'&&url.pathname==='/run'){if(!env.PRICING_RUN_TOKEN||request.headers.get('authorization')!=='Bearer '+env.PRICING_RUN_TOKEN)return new Response('Unauthorized',{status:401});return env.PRICING.getByName('pricing').fetch('https://internal/review');}if(request.method!=='GET'||url.pathname!=='/status')return new Response('Not found',{status:404});return env.PRICING.getByName('pricing').fetch('https://internal/status');}
};
