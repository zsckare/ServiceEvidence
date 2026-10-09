import { useEffect, useState } from 'react';
import { fetchPrivateBlob, type Evidence } from '../../api';
import { Button, Empty } from '../../components/ui/primitives';

/** EN: Object URLs are revoked to avoid retaining sensitive images in memory.
 * ES: Liberamos las URL temporales para no retener imágenes privadas.
 */
function EvidenceImage({ orderId, item }: { orderId: string; item: Evidence }) {
    const [src, setSrc] = useState<string | null>(null);
    const [error, setError] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        let objectUrl: string | null = null;
        setSrc(null);
        setError(false);

        fetchPrivateBlob(`/work-orders/${orderId}/files/${item.id}`, controller.signal)
            .then((blob) => {
                if (controller.signal.aborted) return;
                objectUrl = URL.createObjectURL(blob);
                setSrc(objectUrl);
            })
            .catch(() => { if (!controller.signal.aborted) setError(true); });

        return () => {
            controller.abort();
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [orderId, item.id]);

    if (error) return <div className="evidence-placeholder">Vista previa no disponible</div>;
    if (!src) return <div className="evidence-placeholder">Cargando imagen…</div>;

    return (
        <a href={src} target="_blank" rel="noopener noreferrer" aria-label="Abrir evidencia">
            <img className="evidence-thumbnail" src={src} alt={item.notes || `Evidencia ${item.kind}`} loading="lazy" />
        </a>
    );
}

export function EvidenceGallery({
    orderId,
    items,
    onDownload,
}: {
    orderId: string;
    items: Evidence[];
    onDownload: (item: Evidence) => void;
}) {
    if (items.length === 0) return <Empty>Aún no hay evidencias.</Empty>;

    return (
        <div className="operation-evidence-grid">
            {items.map((item) => (
                <article className="operation-evidence" key={item.id}>
                    {item.contentType.startsWith('image/') ? (
                        <EvidenceImage orderId={orderId} item={item} />
                    ) : (
                        <div className="evidence-placeholder">Documento PDF</div>
                    )}
                    <strong>{item.kind === 'SIGNATURE' ? 'Firma' : item.kind === 'PHOTO' ? 'Fotografía' : 'Documento'}</strong>
                    <span>{item.notes || new Date(item.createdAt).toLocaleString('es-MX')}</span>
                    <Button variant="secondary" onPress={() => onDownload(item)}>Descargar</Button>
                </article>
            ))}
        </div>
    );
}
