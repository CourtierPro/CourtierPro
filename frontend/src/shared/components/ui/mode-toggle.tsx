import * as React from "react"
import { Moon, Sun } from "lucide-react"
import { Switch } from "@/shared/components/ui/switch"
import { useTheme } from "@/app/providers/ThemeProvider"
import { useTranslation } from "react-i18next"

export function ModeToggle() {
    const { theme, setTheme } = useTheme()
    const { t } = useTranslation("common")

    const toggleTheme = (checked: boolean) => {
        const newTheme = checked ? "dark" : "light"

        // @ts-expect-error View Transition API is not yet in all TS types
        if (!document.startViewTransition) {
            setTheme(newTheme)
            return
        }

        // Get the click position from the last interaction or center default
        const x = lastClickPos.current?.x ?? window.innerWidth / 2
        const y = lastClickPos.current?.y ?? window.innerHeight / 2
        const endRadius = Math.hypot(
            Math.max(x, window.innerWidth - x),
            Math.max(y, window.innerHeight - y)
        )

        document.documentElement.style.setProperty('--x', x + 'px')
        document.documentElement.style.setProperty('--y', y + 'px')
        document.documentElement.style.setProperty('--r', endRadius + 'px')

        // @ts-expect-error View Transition API
        document.startViewTransition(() => {
            setTheme(newTheme)
        })
    }

    const lastClickPos = React.useRef<{ x: number, y: number } | null>(null)
    const isDark = theme === "dark" || (theme === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches)

    return (
        <div
            className="flex items-center space-x-2"
            onPointerDown={(e) => {
                lastClickPos.current = { x: e.clientX, y: e.clientY }
            }}
        >
            <div className="relative w-[1.2rem] h-[1.2rem]">
                <Sun className="absolute inset-0 h-[1.2rem] w-[1.2rem] rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0 text-orange-500" />
                <Moon className="absolute inset-0 h-[1.2rem] w-[1.2rem] rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100 dark:text-orange-500" />
            </div>
            <Switch
                id="theme-mode"
                checked={isDark}
                onCheckedChange={toggleTheme}
                aria-label={t("toggleTheme")}
            />
            <span className="sr-only">{t("toggleTheme")}</span>
        </div>
    )
}
