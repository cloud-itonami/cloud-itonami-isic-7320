import test from 'node:test';import assert from 'node:assert/strict';import {readFileSync} from 'node:fs';
const source=readFileSync(new URL('./index.mjs',import.meta.url),'utf8').replace("import {DurableObject} from 'cloudflare:workers';","class DurableObject {constructor(ctx,env){this.ctx=ctx;this.env=env;}}");
const {PricingResident,pricesFromHtml}=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'));
const html='Fixed effort modes Medium $0.10 / request High $0.50 / request X-high $1.00 / request Exa Connect pricing';
test('extracts observed prices; missing source fails',()=>{assert.deepEqual(pricesFromHtml(html).map(x=>x.usd),[.1,.5,1]);assert.throws(()=>pricesFromHtml('missing'),/source-price-not-found/);});
for(const failure of [null,'model','storage','readback','quality'])test('market workflow '+failure,async()=>{
 const rows=new Map(),old=globalThis.fetch;let saved;
 globalThis.fetch=async(url,options={})=>{
  if(String(url).includes('exa.ai'))return new Response(html);
  if(options.headers?.['user-agent']==='Itonami-Quality-Bot/1.0')return Response.json({model:'reviewer-fixture',id:'reviewer-fixture',choices:[{finish_reason:'stop',message:{content:JSON.stringify({accepted:failure!=='quality',reason:'checked'})}}]});
  if(String(url).includes('api.murakumo'))return Response.json({model:'fixture',id:'fixture',choices:[{finish_reason:'stop',message:{content:failure==='model'?'{}':JSON.stringify({price:2,margin:.7,maxCost:.6,costStatus:'unmeasured',demandStatus:'unmeasured',analysis:'API costs are not a finished report.'})}}]});
  if(options.method==='PUT'){saved=options.body;return new Response('',{status:failure==='storage'?503:201});}
  return new Response(failure==='readback'?'corrupt':saved);
 };
 try{const actor=new PricingResident({storage:{put:async(k,v)=>rows.set(k,v),get:async k=>rows.get(k)}},{MURAKUMO_API_KEY:'fixture',KOTOBASE_ARCHIVE_TOKEN:'fixture'});await actor.review(actor.env);const status=rows.get('status');assert.equal(status.state,failure?'failed':'completed');assert.equal(rows.has('lastSuccess'),!failure);if(failure)assert.match(status.error,{model:/invalid-calculation/,storage:/kotobase-write-503/,readback:/kotobase-readback-mismatch/,quality:/quality-rejected/}[failure]);else assert.equal(status.storage.readbackVerified,true);}finally{globalThis.fetch=old;}
});
