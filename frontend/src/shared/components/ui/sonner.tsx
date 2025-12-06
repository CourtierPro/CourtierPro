"use client";

import type React from "react";
import { Toaster as Sonner, type ToasterProps } from "sonner";

const Toaster = ({ theme = "light", ...props }: ToasterProps) => {
  return (
    <Sonner
      theme={theme}
      className="toaster group"
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-border": "var(--border)",
          "--normal-text": "var(--foreground)",
          "--success-bg": "var(--background)",
          "--success-border": "var(--border)",
          "--success-text": "var(--foreground)",
          "--error-bg": "var(--destructive)",
          "--error-border": "var(--destructive)",
          "--error-text": "var(--destructive-foreground)",
        } as React.CSSProperties
      }
      {...props}
    />
  );
};

export { Toaster };
