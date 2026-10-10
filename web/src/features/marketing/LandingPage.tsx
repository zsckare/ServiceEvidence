import { Link } from 'react-router-dom';
import { Brand } from '../../components/ui/primitives';
import {
    ArrowRight,
    ArrowUpRight,
    CheckCircle,
    ClipboardCheck,
    FileCheck02,
    LayersThree01,
    Lock01,
    Menu02,
    ShieldTick,
    Users01,
    XClose,
} from '@untitledui/icons';
import { useState } from 'react';
// @ts-ignore - CSS imports are handled by the bundler/tooling setup
import './landing.css';

const features = [
    { number: '01', icon: ClipboardCheck, title: 'Órdenes y programación', description: 'Crea, programa y da seguimiento a los servicios. Asigna responsables y consulta cada cambio de estado.' },
    { number: '02', icon: FileCheck02, title: 'Evidencia que respalda', description: 'Fotografías, firmas y archivos privados organizados por orden, con reportes PDF para entregar.' },
    { number: '03', icon: Users01, title: 'Equipo conectado', description: 'Usuarios, técnicos y supervisores con roles definidos y una vista de servicios asignados.' },
    { number: '04', icon: LayersThree01, title: 'Clientes y ubicaciones', description: 'Concentra la información de cada cliente, sus sedes y el historial de trabajos realizados.' },
    { number: '05', icon: ShieldTick, title: 'Seguridad y auditoría', description: 'Acceso por organización, permisos por rol y registro de eventos de seguridad.' },
    { number: '06', icon: Lock01, title: 'Planes y control de consumo', description: 'Administra planes, usuarios y límites mensuales desde una consola de plataforma.' },
];

const steps = [
    { number: '01', title: 'Organiza', description: 'Registra clientes, ubicaciones y crea órdenes con su programación.' },
    { number: '02', title: 'Coordina', description: 'Asigna técnicos y supervisa el avance desde el dashboard.' },
    { number: '03', title: 'Documenta', description: 'Adjunta fotografías, firmas y registra cada intervención.' },
    { number: '04', title: 'Entrega', description: 'Genera un reporte PDF y conserva el historial del servicio.' },
];

const audiences = [
    { label: 'Administradores', title: 'Visibilidad para decidir', description: 'Consulta indicadores, estados, órdenes recientes y actividad de la operación.' },
    { label: 'Supervisores', title: 'Coordinación sin fricción', description: 'Da seguimiento a los servicios y mantén al equipo alineado con cada orden.' },
    { label: 'Técnicos', title: 'El trabajo en un solo lugar', description: 'Accede a tus servicios asignados, consulta detalles y registra evidencias desde el móvil.' },
];

/**
 * EN: Public marketing experience, independent of authentication and tenant context.
 * ES: Experiencia comercial pública, independiente de autenticación y contexto de tenant.
 *
 * The dashboard preview below contains fictional illustrative data, not API results.
 * La vista del dashboard contiene datos ilustrativos, no resultados de la API.
 */
export function LandingPage() {
    const [menuOpen, setMenuOpen] = useState(false);

    function closeMenu() {
        setMenuOpen(false);
    }

    return (
        <div className="marketing-site">
            <header className="marketing-header">
                <div className="marketing-container marketing-header-inner">
                    <Link className="marketing-logo" to="/" aria-label="ServiceEvidence — inicio">
                        <Brand />
                    </Link>

                    <button
                        className="marketing-menu-toggle"
                        type="button"
                        aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
                        aria-expanded={menuOpen}
                        aria-controls="marketing-navigation"
                        onClick={() => setMenuOpen(!menuOpen)}
                    >
                        {menuOpen ? <XClose size={22} /> : <Menu02 size={22} />}
                    </button>

                    <nav
                        id="marketing-navigation"
                        className={`marketing-navigation ${menuOpen ? 'is-open' : ''}`}
                        aria-label="Navegación principal"
                    >
                        <a href="#plataforma" onClick={closeMenu}>La plataforma</a>
                        <a href="#funcionalidades" onClick={closeMenu}>Funcionalidades</a>
                        <a href="#como-funciona" onClick={closeMenu}>Cómo funciona</a>
                        <a href="#equipos" onClick={closeMenu}>Para tu equipo</a>
                        <Link className="marketing-header-cta" to="/login" onClick={closeMenu}>
                            Iniciar sesión <ArrowUpRight size={16} />
                        </Link>
                    </nav>
                </div>
            </header>

            <main>
                <section className="marketing-hero" id="plataforma">
                    <div className="marketing-container marketing-hero-content">
                        <div className="marketing-hero-copy">
                            <div className="marketing-pill">
                                <span className="marketing-pill-dot" />
                                PLATAFORMA DE OPERACIONES EN CAMPO
                                <ArrowRight size={14} />
                            </div>
                            <h1>
                                Cada servicio, <span>bajo control y con evidencia.</span>
                            </h1>
                            <p className="marketing-lead">
                                Centraliza clientes, órdenes, técnicos y evidencias en una sola plataforma.
                                Supervisa tu operación, documenta cada visita y entrega reportes profesionales
                                sin perder el contexto.
                            </p>
                            <div className="marketing-hero-actions">
                                <a className="marketing-button marketing-button-primary" href="#funcionalidades">
                                    Explorar la plataforma <ArrowRight size={18} />
                                </a>
                                <Link className="marketing-button marketing-button-outline" to="/login">
                                    Acceder a mi cuenta
                                </Link>
                            </div>
                            <div className="marketing-assurances">
                                <span><CheckCircle size={17} /> Gestión multiempresa</span>
                                <span><CheckCircle size={17} /> Técnicos y evidencias</span>
                                <span><CheckCircle size={17} /> Reportes y auditoría</span>
                            </div>
                        </div>

                        <div className="marketing-hero-visual">
                            <div className="marketing-orbit marketing-orbit-one" />
                            <div className="marketing-orbit marketing-orbit-two" />
                            <DashboardPreview />
                            <div className="marketing-float-card marketing-float-top">
                                <span className="marketing-float-icon"><FileCheck02 size={20} /></span>
                                <span><strong>Reporte generado</strong><small>Listo para entregar</small></span>
                                <CheckCircle size={18} className="marketing-check-icon" />
                            </div>
                            <div className="marketing-float-card marketing-float-bottom">
                                <span className="marketing-float-avatar">SE</span>
                                <span><strong>Evidencia registrada</strong><small>Fotografía y firma adjuntas</small></span>
                            </div>
                        </div>
                    </div>
                    <div className="marketing-container marketing-hero-footer">
                        <span>DISEÑADO PARA EQUIPOS QUE EXIGEN MÁS</span>
                        <div><Lock01 size={16} /> Control de acceso <span className="marketing-footer-divider" /> <Users01 size={16} /> Trabajo colaborativo <span className="marketing-footer-divider" /> <ClipboardCheck size={16} /> Trazabilidad</div>
                    </div>
                </section>

                <section className="marketing-section marketing-features-section" id="funcionalidades">
                    <div className="marketing-container">
                        <div className="marketing-section-heading">
                            <div>
                                <span className="marketing-overline">TODO EL CICLO DE SERVICIO</span>
                                <h2>Todo lo que tu equipo necesita.<br /><em>Una operación más conectada.</em></h2>
                            </div>
                            <p>Desde la planeación hasta la entrega del reporte, mantén conectadas a las personas, la información y las evidencias.</p>
                        </div>
                        <div className="marketing-feature-grid">
                            {features.map((feature) => {
                                const Icon = feature.icon;
                                return (
                                    <article className="marketing-feature-card" key={feature.number}>
                                        <div className="marketing-feature-top">
                                            <span className="marketing-feature-icon"><Icon size={24} /></span>
                                            <span className="marketing-feature-number">{feature.number} / 06</span>
                                        </div>
                                        <h3>{feature.title}</h3>
                                        <p>{feature.description}</p>
                                    </article>
                                );
                            })}
                        </div>
                    </div>
                </section>

                <section className="marketing-showcase-section">
                    <div className="marketing-container marketing-showcase-grid">
                        <div className="marketing-showcase-copy">
                            <span className="marketing-overline">SEGUIMIENTO OPERATIVO</span>
                            <h2>Del trabajo en campo <em>a la evidencia entregada.</em></h2>
                            <p>Olvídate de buscar evidencias en conversaciones dispersas. Cada orden reúne su responsable, estado, historial y documentación.</p>
                            <ul>
                                <li><CheckCircle size={19} /> Seguimiento del ciclo de vida de las órdenes</li>
                                <li><CheckCircle size={19} /> Evidencias asociadas a cada intervención</li>
                                <li><CheckCircle size={19} /> Acceso según roles y organización</li>
                            </ul>
                            <a href="#como-funciona" className="marketing-text-link">Conoce el flujo de trabajo <ArrowRight size={18} /></a>
                        </div>
                        <div className="marketing-showcase-art" aria-label="Representación ilustrativa de un expediente de servicio">
                            <div className="marketing-evidence-card">
                                <div className="marketing-evidence-header"><span>EXPEDIENTE DE SERVICIO</span><span className="marketing-status">Completado</span></div>
                                <h3>Inspección de instalaciones</h3>
                                <p>Orden de trabajo · SE-1042</p>
                                <div className="marketing-evidence-photo"><ClipboardCheck size={44} /><span>Registro fotográfico del servicio</span></div>
                                <div className="marketing-evidence-row"><span>Fotografías adjuntas</span><strong>03</strong></div>
                                <div className="marketing-evidence-row"><span>Firma de conformidad</span><strong>Registrada ✓</strong></div>
                                <div className="marketing-evidence-footer"><FileCheck02 size={18} /> Reporte PDF disponible <ArrowUpRight size={17} /></div>
                            </div>
                        </div>
                    </div>
                </section>

                <section className="marketing-section marketing-process-section" id="como-funciona">
                    <div className="marketing-container">
                        <div className="marketing-process-heading">
                            <span className="marketing-overline">UN FLUJO DE PRINCIPIO A FIN</span>
                            <h2>De la planeación al reporte.<br /><em>Sin pasos perdidos.</em></h2>
                        </div>
                        <div className="marketing-process-grid">
                            {steps.map((step) => (
                                <article key={step.number} className="marketing-process-step">
                                    <span className="marketing-step-number">{step.number}</span>
                                    <h3>{step.title}</h3>
                                    <p>{step.description}</p>
                                </article>
                            ))}
                        </div>
                    </div>
                </section>

                <section className="marketing-section marketing-audience-section" id="equipos">
                    <div className="marketing-container">
                        <div className="marketing-section-heading">
                            <div>
                                <span className="marketing-overline">UNA EXPERIENCIA PARA CADA ROL</span>
                                <h2>Tu equipo conectado.<br /><em>Cada quien con lo que necesita.</em></h2>
                            </div>
                            <p>Un mismo flujo de servicio con vistas y permisos adecuados para la operación diaria.</p>
                        </div>
                        <div className="marketing-audience-grid">
                            {audiences.map((audience) => (
                                <article className="marketing-audience-card" key={audience.label}>
                                    <span className="marketing-audience-label">{audience.label}</span>
                                    <h3>{audience.title}</h3>
                                    <p>{audience.description}</p>
                                </article>
                            ))}
                        </div>
                        <div className="marketing-platform-note">
                            <span className="marketing-platform-note-icon"><ShieldTick size={24} /></span>
                            <div>
                                <strong>Diseñado para operar con varias empresas</strong>
                                <p>Información separada por organización, gestión de usuarios y roles, auditoría y administración central de planes y cuotas.</p>
                            </div>
                        </div>
                    </div>
                </section>

                <section className="marketing-cta-section">
                    <div className="marketing-container">
                        <div className="marketing-cta-panel">
                            <div className="marketing-cta-glow" />
                            <span className="marketing-overline">SERVICEEVIDENCE</span>
                            <h2>Tu trabajo habla por sí solo.<br /><em>Ahora también tiene evidencia.</em></h2>
                            <p>Órdenes, técnicos, clientes, evidencias y reportes reunidos en una plataforma profesional.</p>
                            <Link className="marketing-button marketing-button-light" to="/login">
                                Ingresar a la plataforma <ArrowUpRight size={18} />
                            </Link>
                        </div>
                    </div>
                </section>
            </main>

            <footer className="marketing-footer">
                <div className="marketing-container marketing-footer-inner">
                    <Brand />
                    <span>© {new Date().getFullYear()} ServiceEvidence. Todos los derechos reservados.</span>
                    <a href="#plataforma">Volver arriba ↑</a>
                </div>
            </footer>
        </div>
    );
}

/** EN/ES: Static visual demo; no real tenant information is displayed. */
function DashboardPreview() {
    const rows = [
        { title: 'Mantenimiento preventivo', code: 'SE-1042', status: 'En proceso', tone: 'progress' },
        { title: 'Inspección de instalaciones', code: 'SE-1041', status: 'Completada', tone: 'complete' },
        { title: 'Servicio correctivo', code: 'SE-1040', status: 'Programada', tone: 'scheduled' },
    ];

    return (
        <div className="marketing-dashboard">
            <div className="marketing-dashboard-sidebar">
                <div className="marketing-dashboard-brand"><ClipboardCheck size={21} /><span>SE</span></div>
                <div className="marketing-sidebar-line is-active" />
                <div className="marketing-sidebar-line" />
                <div className="marketing-sidebar-line" />
                <div className="marketing-sidebar-line is-short" />
            </div>
            <div className="marketing-dashboard-main">
                <div className="marketing-dashboard-toolbar"><span>Panel de operaciones</span><span className="marketing-dashboard-user">AD</span></div>
                <div className="marketing-dashboard-body">
                    <span className="marketing-dashboard-kicker">RESUMEN GENERAL</span>
                    <h2>Buen día, administrador <span>✳</span></h2>
                    <p>Así se encuentran tus operaciones.</p>
                    <div className="marketing-stat-grid">
                        <div><small>Órdenes totales</small><strong>128</strong><span>↗ +12 este mes</span></div>
                        <div><small>En proceso</small><strong>24</strong><span>En seguimiento</span></div>
                        <div><small>Completadas</small><strong>104</strong><span>✓ Documentadas</span></div>
                    </div>
                    <div className="marketing-dashboard-table">
                        <div className="marketing-table-title"><strong>Órdenes recientes</strong><span>Ver todas →</span></div>
                        {rows.map((row) => (
                            <div className="marketing-table-row" key={row.code}>
                                <span className="marketing-table-icon"><ClipboardCheck size={15} /></span>
                                <span className="marketing-table-name"><strong>{row.title}</strong><small>{row.code}</small></span>
                                <span className={`marketing-table-status ${row.tone}`}>{row.status}</span>
                            </div>
                        ))}
                    </div>
                    <div className="marketing-dashboard-disclaimer">Vista ilustrativa · Datos de ejemplo</div>
                </div>
            </div>
        </div>
    );
}
