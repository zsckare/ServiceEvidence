// EN: Access token remains in memory, not persistent browser storage.
// ES: El token de acceso vive en memoria, no en almacenamiento persistente.
export type TokenPair={accessToken:string;refreshToken:string;tokenType:string};
export type User={id:string;email:string;fullName:string;platformAdmin:boolean};
export type Page<T>={content:T[];totalElements:number;totalPages:number;number:number};
export type WorkOrder={id:string;title:string;status:string;priority:string;customerId:string;locationId:string;createdAt:string;scheduledAt:string|null};
export type Customer={id:string;name:string;email:string|null;phone:string|null;active:boolean};
export type Notification={id:string;title:string;message:string;createdAt:string;readAt:string|null};
let token:string|null=null;
export function setAccessToken(value:string|null){token=value;}
export function getAccessToken(){return token;}
export async function request<T>(path:string,init:RequestInit={},bearer=token):Promise<T>{
 const headers=new Headers(init.headers);
 if(bearer)headers.set('Authorization',`Bearer ${bearer}`);
 if(init.body && !(init.body instanceof FormData))headers.set('Content-Type','application/json');
 const response=await fetch(`/api/v1${path}`,{...init,headers});
 if(!response.ok){const detail=await response.text().catch(()=> '');throw new Error(`${response.status}: ${detail.slice(0,200) || response.statusText}`);}
 if(response.status===204)return undefined as T;
 return response.json() as Promise<T>;
}
export const auth={login:(email:string,password:string)=>request<TokenPair>('/auth/login',{method:'POST',body:JSON.stringify({email,password})},null),me:()=>request<User>('/auth/me'),tenants:()=>request<string[]>('/auth/available-tenants'),select:(tenantId:string)=>request<{accessToken:string}>('/auth/tenant-context',{method:'POST',body:JSON.stringify({tenantId})})};
