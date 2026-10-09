// EN: JWT tokens stay in memory, not localStorage.
// ES: Los JWT permanecen en memoria, nunca en localStorage.
export type TokenPair = {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
};

export type User = {
    id: string;
    email: string;
    fullName: string;
    platformAdmin: boolean;
};

export type Page<T> = {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
};

export type WorkOrder = {
    id: string;
    title: string;
    status: string;
    priority: string;
    customerId: string;
    locationId: string;
    createdAt: string;
    scheduledAt: string | null;
};

export type Customer = {
    id: string;
    name: string;
    email: string | null;
    phone: string | null;
    active: boolean;
};

export type Notification = {
    id: string;
    title: string;
    message: string;
    createdAt: string;
    readAt: string | null;
};

export type TenantInfo = {
    id: string;
    code: string;
    name: string;
    active: boolean;
};

let token: string | null = null;

export function setAccessToken(value: string | null) {
    token = value;
}

export function getAccessToken() {
    return token;
}

/** EN: Shared JSON API client with optional bearer token.
 *  ES: Cliente JSON común con autenticación Bearer opcional.
 */
export async function request<T>(
    path: string,
    init: RequestInit = {},
    bearer: string | null = token,
): Promise<T> {
    const headers = new Headers(init.headers);

    if (bearer) headers.set('Authorization', `Bearer ${bearer}`);
    if (init.body && !(init.body instanceof FormData)) {
        headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(`/api/v1${path}`, { ...init, headers });
    if (!response.ok) {
        const detail = await response.text().catch(() => '');
        throw new Error(`${response.status}: ${detail.slice(0, 200) || response.statusText}`);
    }

    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
}

export const auth = {
    login: (email: string, password: string) =>
        request<TokenPair>('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password }),
        }, null),
    me: () => request<User>('/auth/me'),
    tenants: () => request<string[]>('/auth/available-tenants'),
    select: (tenantId: string) =>
        request<{ accessToken: string }>('/auth/tenant-context', {
            method: 'POST',
            body: JSON.stringify({ tenantId }),
        }),
};

// EN: Only platform admins are authorized to list all tenants.
// ES: Solo administradores globales pueden listar todas las empresas.
export const platform = {
    tenants: () => request<TenantInfo[]>('/platform/tenants'),
};

// EN: DTOs match Spring Boot controllers. ES: Contratos del backend actual.
export type CustomerInput = {
    name: string; legalName: string | null; taxId: string | null;
    email: string | null; phone: string | null; notes: string | null; active: boolean;
};
export type Location = {
    id: string; customerId: string; name: string; addressLine1: string;
    addressLine2: string | null; city: string; state: string | null;
    postalCode: string | null; country: string; contactName: string | null;
    contactPhone: string | null; notes: string | null; active: boolean;
};
export type LocationInput = Omit<Location, 'id' | 'customerId'>;
export type WorkOrderInput = {
    title: string; customerId: string; locationId: string;
    description: string | null; priority: string; scheduledAt: string | null;
};
export type Evidence = {
    id: string; workOrderId: string; kind: string; contentType: string;
    notes: string | null; createdAt: string; byteSize: number;
};
export type WorkOrderEvent = {
    action: string; details: string | null; createdAt: string;
};

/** EN: Downloads private files with JWT; never exposes the bearer in URLs.
 * ES: Descarga archivos privados sin poner el JWT en la URL.
 */
export async function downloadPrivateFile(path: string, filename: string): Promise<void> {
    const headers = new Headers();
    const accessToken = getAccessToken();
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
    const response = await fetch(`/api/v1${path}`, { headers });
    if (!response.ok) throw new Error(`Error al descargar (${response.status}).`);
    const url = URL.createObjectURL(await response.blob());
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
