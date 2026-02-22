import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";
import { useTranslation } from "react-i18next";
import { motion, useReducedMotion } from "framer-motion";
import { FileText, FolderOpen, Calendar, BarChart2, Building2, Users, ArrowRight } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { Card, CardContent } from "@/shared/components/ui/card";
import { getRoleFromUser } from "@/features/auth/roleUtils";
import { useLanguage } from "@/app/providers/LanguageContext";

// ── Design constants (hero is always dark regardless of user's theme) ─────────
const HERO_BG  = "oklch(0.1371 0.0360 258.5258)";                      // deep navy
const AMBER    = "oklch(0.5746 0.2020 44.4414)";                        // brand orange/amber
const EASE_OUT: [number, number, number, number] = [0.22, 1, 0.36, 1]; // snappy cubic-bezier

// ── Feature cards ──────────────────────────────────────────────────────────────
const FEATURES = [
    { Icon: FileText,   num: "01", titleKey: "features.transactions.title" as const, descKey: "features.transactions.desc" as const },
    { Icon: FolderOpen, num: "02", titleKey: "features.documents.title"    as const, descKey: "features.documents.desc"    as const },
    { Icon: Calendar,   num: "03", titleKey: "features.appointments.title" as const, descKey: "features.appointments.desc" as const },
    { Icon: BarChart2,  num: "04", titleKey: "features.analytics.title"    as const, descKey: "features.analytics.desc"    as const },
];

// Skyline columns: [widthPct, heightPct, leftOffsetPct, floatDuration, floatDelay]
const SKYLINE: Array<[number, number, number, number, number]> = [
    [ 5,  48,  1.5, 4.3, 0.0],
    [ 6,  72,  8.0, 3.8, 0.4],
    [ 4,  36, 16.0, 4.5, 0.9],
    [ 7,  88, 22.0, 3.5, 0.2],
    [ 5,  60, 31.0, 4.1, 0.6],
    [ 6,  78, 39.0, 3.7, 1.0],
    [ 4,  42, 47.0, 4.4, 0.3],
    [ 8, 100, 53.0, 3.9, 0.7],
    [ 5,  65, 63.0, 4.0, 0.1],
    [ 6,  82, 71.0, 3.6, 0.5],
    [ 4,  50, 80.0, 4.2, 0.8],
    [ 5,  70, 87.0, 3.8, 0.2],
    [ 6,  56, 94.0, 4.1, 0.6],
];

// ── HeroGeometry: animated architectural skyline columns ───────────────────────
function HeroGeometry({ reduced }: { reduced: boolean }) {
    return (
        <>
            {SKYLINE.map(([w, h, left, dur, delay], i) => (
                <motion.div
                    key={i}
                    style={{
                        position:        "absolute",
                        bottom:          0,
                        left:            `${left}%`,
                        width:           `${w}%`,
                        height:          `${h}%`,
                        background:      `linear-gradient(to top, ${AMBER}22, ${AMBER}07)`,
                        borderTop:       `1px solid ${AMBER}48`,
                        borderLeft:      `1px solid ${AMBER}26`,
                        borderRight:     `1px solid ${AMBER}26`,
                        transformOrigin: "center bottom",
                    }}
                    initial={{ scaleY: 0, opacity: 0 }}
                    animate={
                        reduced
                            ? { scaleY: 1, opacity: 0.75 }
                            : { scaleY: 1, opacity: 0.75, y: [0, -8, 0] }
                    }
                    transition={
                        reduced
                            ? { duration: 0.3, delay: i * 0.02 }
                            : {
                                  scaleY:  { duration: 0.9, delay: delay + 0.3, ease: EASE_OUT },
                                  opacity: { duration: 0.6, delay: delay + 0.3 },
                                  y: {
                                      duration:   dur,
                                      delay:      delay + 1.5,
                                      repeat:     Infinity,
                                      ease:       "easeInOut",
                                      repeatType: "mirror",
                                  },
                              }
                    }
                />
            ))}
        </>
    );
}

// ── LandingPage ────────────────────────────────────────────────────────────────
export function LandingPage() {
    const { isLoading, isAuthenticated, user, loginWithRedirect } = useAuth0();
    const navigate = useNavigate();
    const { t, i18n } = useTranslation("landing");
    const { language, setLanguage } = useLanguage();
    const prefersReduced = useReducedMotion();
    const reduced = prefersReduced ?? false;

    useEffect(() => {
        if (!isLoading && isAuthenticated && user) {
            const role = getRoleFromUser(user);
            if (role) {
                const dashboards: Record<typeof role, string> = {
                    broker: "/dashboard/broker",
                    client: "/dashboard/client",
                    admin:  "/dashboard/admin",
                };
                navigate(dashboards[role], { replace: true });
            }
        }
    }, [isLoading, isAuthenticated, user, navigate]);

    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-background">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
        );
    }

    const handleLogin = () => loginWithRedirect({ appState: { returnTo: "/" } });

    const handleLanguageChange = (lang: "en" | "fr") => {
        setLanguage(lang);
        i18n.changeLanguage(lang);
    };

    // Hero entrance animation (fades in from below)
    const heroAnim = (delay: number) => ({
        initial:    { opacity: reduced ? 1 : 0, y: reduced ? 0 : 28 },
        animate:    { opacity: 1, y: 0 },
        transition: { duration: reduced ? 0 : 0.75, delay: reduced ? 0 : delay, ease: EASE_OUT },
    });

    // Scroll-triggered reveal (fades in from below when entering viewport)
    const revealAnim = (delay = 0) => ({
        initial:     { opacity: reduced ? 1 : 0, y: reduced ? 0 : 40 },
        whileInView: reduced ? undefined : { opacity: 1, y: 0 },
        animate:     reduced ? { opacity: 1, y: 0 } : undefined,
        viewport:    { once: true, margin: "-80px" },
        transition:  { duration: reduced ? 0 : 0.7, delay: reduced ? 0 : delay, ease: EASE_OUT },
    });

    return (
        <div className="min-h-screen bg-background text-foreground">
            {/* ── Skip-to-content (WCAG 2.1 SC 2.4.1) ── */}
            <a
                href="#main-content"
                className="sr-only focus:not-sr-only focus:absolute focus:top-0 focus:left-0 focus:z-[100] focus:p-4 focus:bg-background focus:text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
            >
                {t("skipToContent")}
            </a>

            {/* ── Sticky nav ── */}
            <header className="sticky top-0 z-50 border-b bg-background/80 backdrop-blur-sm">
                <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
                    {/* Left: brand + nav links */}
                    <div className="flex items-center gap-8">
                        <button
                            onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
                            className="text-xl font-bold text-primary focus:outline-none focus:ring-2 focus:ring-ring rounded"
                            style={{ fontFamily: "'Playfair Display', Georgia, serif" }}
                        >
                            CourtierPro
                        </button>
                        <nav aria-label={t("nav.ariaLabel")} className="hidden items-center gap-6 md:flex">
                            <a
                                href="#features"
                                className="text-sm text-muted-foreground transition-colors hover:text-foreground focus:outline-none focus:ring-2 focus:ring-ring rounded"
                            >
                                {t("nav.features")}
                            </a>
                            <a
                                href="#for-you"
                                className="text-sm text-muted-foreground transition-colors hover:text-foreground focus:outline-none focus:ring-2 focus:ring-ring rounded"
                            >
                                {t("nav.forYou")}
                            </a>
                        </nav>
                    </div>

                    {/* Right: language toggle + sign in */}
                    <div className="flex items-center gap-3">
                        <div className="flex items-center gap-1" role="group" aria-label="Language selection">
                            <button
                                onClick={() => handleLanguageChange("en")}
                                className="rounded border px-2 py-1 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-ring data-[active=true]:border-border data-[active=true]:text-foreground data-[active=false]:border-transparent data-[active=false]:text-muted-foreground data-[active=false]:hover:text-foreground"
                                data-active={language === "en"}
                                aria-label="Switch to English"
                                aria-pressed={language === "en"}
                            >
                                EN
                            </button>
                            <button
                                onClick={() => handleLanguageChange("fr")}
                                className="rounded border px-2 py-1 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-ring data-[active=true]:border-border data-[active=true]:text-foreground data-[active=false]:border-transparent data-[active=false]:text-muted-foreground data-[active=false]:hover:text-foreground"
                                data-active={language === "fr"}
                                aria-label="Passer en français"
                                aria-pressed={language === "fr"}
                            >
                                FR
                            </button>
                        </div>

                        <Button onClick={handleLogin} size="sm">
                            {t("nav.signIn")}
                        </Button>
                    </div>
                </div>
            </header>

            <main id="main-content" tabIndex={-1} className="focus:outline-none">
                {/* ── Hero ── */}
                <section
                    className="relative flex min-h-[85vh] flex-col items-center justify-center overflow-hidden px-6 py-24"
                    style={{ backgroundColor: HERO_BG }}
                    aria-labelledby="hero-heading"
                >
                    {/* Animated skyline columns — decorative */}
                    <div className="absolute inset-0 overflow-hidden" aria-hidden="true">
                        <HeroGeometry reduced={reduced} />
                    </div>

                    {/* Radial amber glow centred behind text for readability */}
                    <div
                        className="absolute inset-0 pointer-events-none"
                        style={{
                            background: `radial-gradient(ellipse 80% 65% at 50% 50%, ${AMBER}14 0%, transparent 65%)`,
                        }}
                        aria-hidden="true"
                    />

                    {/* Content */}
                    <div className="relative z-10 mx-auto max-w-3xl text-center">
                        {/* Thin amber accent rule */}
                        <motion.div
                            className="mx-auto mb-8 h-px w-16"
                            style={{ backgroundColor: AMBER, transformOrigin: "center" }}
                            initial={{ scaleX: reduced ? 1 : 0, opacity: reduced ? 1 : 0 }}
                            animate={{ scaleX: 1, opacity: 1 }}
                            transition={{ duration: reduced ? 0 : 0.6, ease: EASE_OUT }}
                        />

                        {/* Headline — Playfair Display serif */}
                        <motion.h1
                            id="hero-heading"
                            className="text-4xl font-bold leading-tight tracking-tight text-white sm:text-5xl lg:text-6xl"
                            style={{ fontFamily: "'Playfair Display', Georgia, serif" }}
                            {...heroAnim(0.15)}
                        >
                            {t("hero.headline")}
                        </motion.h1>

                        {/* Amber underline accent below headline */}
                        <motion.div
                            className="mx-auto mt-5 h-1 w-20 rounded-full"
                            style={{ backgroundColor: AMBER, transformOrigin: "center" }}
                            initial={{ scaleX: reduced ? 1 : 0 }}
                            animate={{ scaleX: 1 }}
                            transition={{ duration: reduced ? 0 : 0.7, delay: reduced ? 0 : 0.75, ease: EASE_OUT }}
                        />

                        {/* Subtext */}
                        <motion.p
                            className="mt-8 text-lg leading-relaxed text-white/80 sm:text-xl"
                            {...heroAnim(0.35)}
                        >
                            {t("hero.subtext")}
                        </motion.p>

                        {/* CTA */}
                        <motion.div className="mt-10" {...heroAnim(0.55)}>
                            <Button
                                onClick={handleLogin}
                                size="lg"
                                className="bg-white px-8 font-semibold text-primary hover:bg-white/90 gap-2"
                            >
                                {t("hero.cta")}
                                <ArrowRight className="h-4 w-4" aria-hidden="true" />
                            </Button>
                        </motion.div>
                    </div>
                </section>

                {/* ── Feature highlights ── */}
                <section id="features" className="px-6 py-24" aria-labelledby="features-heading">
                    <div className="mx-auto max-w-6xl">
                        <motion.h2
                            id="features-heading"
                            className="text-center text-3xl font-bold tracking-tight"
                            {...revealAnim(0)}
                        >
                            {t("features.title")}
                        </motion.h2>
                        <motion.p
                            className="mt-4 text-center text-muted-foreground"
                            {...revealAnim(0.1)}
                        >
                            {t("features.subtitle")}
                        </motion.p>

                        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
                            {FEATURES.map(({ Icon, num, titleKey, descKey }, idx) => (
                                <motion.div key={titleKey} {...revealAnim(idx * 0.1)}>
                                    <Card className="relative overflow-hidden border bg-card h-full">
                                        <CardContent className="p-6">
                                            {/* Watermark number in Playfair Display */}
                                            <span
                                                className="absolute -right-2 -top-4 select-none text-8xl font-bold leading-none text-muted-foreground/10 pointer-events-none"
                                                style={{ fontFamily: "'Playfair Display', Georgia, serif" }}
                                                aria-hidden="true"
                                            >
                                                {num}
                                            </span>
                                            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10 text-primary">
                                                <Icon className="h-6 w-6" aria-hidden="true" />
                                            </div>
                                            <h3 className="font-semibold">{t(titleKey)}</h3>
                                            <p className="mt-2 text-sm text-muted-foreground">{t(descKey)}</p>
                                        </CardContent>
                                    </Card>
                                </motion.div>
                            ))}
                        </div>
                    </div>
                </section>

                {/* ── Who it's for ── */}
                <section id="for-you" className="bg-muted/40 px-6 py-24" aria-labelledby="for-you-heading">
                    <div className="mx-auto max-w-6xl">
                        <motion.h2
                            id="for-you-heading"
                            className="text-center text-3xl font-bold tracking-tight"
                            {...revealAnim(0)}
                        >
                            {t("forYou.title")}
                        </motion.h2>

                        <div className="mt-12 grid gap-8 md:grid-cols-2">
                            {/* Broker card — slides in from left */}
                            <motion.div
                                initial={{ opacity: reduced ? 1 : 0, x: reduced ? 0 : -48 }}
                                whileInView={reduced ? undefined : { opacity: 1, x: 0 }}
                                animate={reduced ? { opacity: 1, x: 0 } : undefined}
                                viewport={{ once: true, margin: "-80px" }}
                                transition={{ duration: reduced ? 0 : 0.7, delay: reduced ? 0 : 0.1, ease: EASE_OUT }}
                            >
                                <Card className="border bg-card h-full">
                                    <CardContent className="p-8">
                                        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-primary">
                                            <Building2 className="h-7 w-7" aria-hidden="true" />
                                        </div>
                                        <h3 className="text-xl font-bold">{t("forYou.broker.title")}</h3>
                                        <p className="mt-3 leading-relaxed text-muted-foreground">
                                            {t("forYou.broker.desc")}
                                        </p>
                                    </CardContent>
                                </Card>
                            </motion.div>

                            {/* Client card — slides in from right */}
                            <motion.div
                                initial={{ opacity: reduced ? 1 : 0, x: reduced ? 0 : 48 }}
                                whileInView={reduced ? undefined : { opacity: 1, x: 0 }}
                                animate={reduced ? { opacity: 1, x: 0 } : undefined}
                                viewport={{ once: true, margin: "-80px" }}
                                transition={{ duration: reduced ? 0 : 0.7, delay: reduced ? 0 : 0.2, ease: EASE_OUT }}
                            >
                                <Card className="border bg-card h-full">
                                    <CardContent className="p-8">
                                        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-primary">
                                            <Users className="h-7 w-7" aria-hidden="true" />
                                        </div>
                                        <h3 className="text-xl font-bold">{t("forYou.client.title")}</h3>
                                        <p className="mt-3 leading-relaxed text-muted-foreground">
                                            {t("forYou.client.desc")}
                                        </p>
                                    </CardContent>
                                </Card>
                            </motion.div>
                        </div>
                    </div>
                </section>
            </main>

            {/* ── Footer ── */}
            <footer className="border-t px-6 py-8">
                <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 sm:flex-row">
                    <p className="text-sm text-muted-foreground">
                        {t("footer.copyright", { year: new Date().getFullYear() })}
                    </p>
                    <span className="text-sm text-muted-foreground">{t("footer.location")}</span>
                </div>
            </footer>
        </div>
    );
}
