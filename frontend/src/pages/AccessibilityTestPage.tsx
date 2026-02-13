import { useState } from "react";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import { Alert, AlertDescription, AlertTitle } from "@/shared/components/ui/alert";
import { Section } from "@/shared/components/branded/Section";
import { AlertCircle, Star, Settings, ExternalLink } from "lucide-react";
import { toast } from "sonner";

export function AccessibilityTestPage() {
  const [alertVisible, setAlertVisible] = useState(false);
  const [isStarred, setIsStarred] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="space-y-8 p-8 max-w-4xl mx-auto">
      <div className="space-y-4">
        <h1 className="text-3xl font-bold tracking-tight">Accessibility Compliance Test</h1>
        <p className="text-muted-foreground text-lg">
          Use this page to verify WCAG AA compliance features including focus states, contrast, semantic structure, and screen reader support.
        </p>
      </div>

      <Section title="1. Semantic Headings (H1 -> H2)" description="Verify that this section parses as an H2, not an H4.">
        <div className="space-y-4">
          <p>The title of this card should be an <code>&lt;h2&gt;</code> element.</p>
          <div className="p-4 border rounded-md">
            <h3 className="text-lg font-semibold mb-2">This is an H3 Heading</h3>
            <p>Proper nesting: H1 (Page Title) → H2 (Section Title) → H3 (Subsection).</p>
          </div>
        </div>
      </Section>

      <Section title="2. Global Focus States" description="Tab through these elements to verify the visible focus ring.">
        <div className="flex flex-wrap gap-4">
          <Button>Primary Button</Button>
          <Button variant="secondary">Secondary Button</Button>
          <Button variant="outline">Outline Button</Button>
          <Button variant="ghost">Ghost Button</Button>
          <Button variant="destructive">Destructive Button</Button>
        </div>
        <div className="mt-4 max-w-sm">
          <Label htmlFor="focus-input">Focusable Input</Label>
          <Input id="focus-input" placeholder="Tab to me..." />
        </div>
      </Section>

      <Section
        title="3. Semantic Links vs Buttons"
        description={
          <>
            Verify links use <code>&lt;a&gt;</code> and buttons use <code>&lt;button&gt;</code>.
          </>
        }
      >
        <div className="flex gap-4 items-center">
          <Button onClick={() => alert("Action triggered!")}>
            I am a &lt;button&gt; (Action)
          </Button> 
          
          <a href="#" className="text-primary underline hover:no-underline focus:ring-2 focus:ring-ring rounded-sm p-1">
            I am an &lt;a&gt; link (Navigation)
          </a>
        </div>
      </Section>

      <Section title="4. Live Regions & Alerts" description="Trigger alerts to test screen reader announcements.">
        <div className="space-y-4">
          <div className="flex gap-4">
            <Button onClick={() => setAlertVisible(!alertVisible)}>
              {alertVisible ? "Hide Alert" : "Show Alert"}
            </Button>
            <Button variant="outline" onClick={() => toast.success("Action successful!")}>
              Trigger Toast Notification
            </Button>
          </div>

          {alertVisible && (
            <Alert role="alert">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Attention Needed</AlertTitle>
              <AlertDescription>
                This is a live region. Screen readers should announce this immediately when it appears.
              </AlertDescription>
            </Alert>
          )}
        </div>
      </Section>

      <Section title="5. Form Labels" description="Click the label text 'Email Address' to verify it focuses the input.">
        <form className="space-y-4 max-w-md p-4 border rounded-md">
          <div className="grid gap-2">
            <Label htmlFor="email">Email Address</Label>
            <Input id="email" type="email" placeholder="name@example.com" />
            <p className="text-sm text-muted-foreground">This helper text should be associated programmatically if validation fails.</p>
          </div>
          
          <div className="flex items-center space-x-2">
            <input type="checkbox" id="terms" className="h-4 w-4 rounded border-gray-300 focus:ring-primary" />
            <Label htmlFor="terms">I agree to the terms and conditions</Label>
          </div>
        </form>
      </Section>

      <Section title="6. ARIA Labels & Interactive State" description="Verify screen readers announce the label and state of these controls.">
        <div className="grid gap-6 md:grid-cols-2">
          {/* Case 1: Icon-only Button */}
          <div className="p-4 border rounded-md space-y-2">
            <h3 className="font-medium text-sm text-muted-foreground">Icon-Only Button (requires aria-label)</h3>
            <div className="flex items-center gap-2">
              <Button size="icon" variant="outline" aria-label="Settings">
                <Settings className="h-4 w-4" />
              </Button>
              <span className="text-xs text-muted-foreground">Inspect me: I have `aria-label="Settings"`</span>
            </div>
          </div>

          {/* Case 2: Toggle Button */}
          <div className="p-4 border rounded-md space-y-2">
            <h3 className="font-medium text-sm text-muted-foreground">Toggle State (requires aria-pressed)</h3>
            <div className="flex items-center gap-2">
              <Button 
                size="icon" 
                variant={isStarred ? "default" : "outline"}
                onClick={() => setIsStarred(!isStarred)}
                aria-pressed={isStarred}
                aria-label="Mark as favorite"
              >
                <Star className={`h-4 w-4 ${isStarred ? "fill-current" : ""}`} />
              </Button>
              <span className="text-xs text-muted-foreground">State: {isStarred ? "Pressed" : "Not Pressed"}</span>
            </div>
          </div>

          {/* Case 3: Disclosure */}
          <div className="p-4 border rounded-md space-y-2">
            <h3 className="font-medium text-sm text-muted-foreground">Disclosure (requires aria-expanded)</h3>
            <div>
              <Button 
                variant="ghost" 
                onClick={() => setIsExpanded(!isExpanded)}
                aria-expanded={isExpanded}
                aria-controls="disclosure-content"
                className="w-full justify-between"
              >
                <span>Click to expand</span>
                <ExternalLink className={`h-4 w-4 transition-transform ${isExpanded ? "rotate-180" : ""}`} />
              </Button>
              {isExpanded && (
                <div id="disclosure-content" className="p-2 bg-muted mt-2 rounded text-sm animate-in fade-in slide-in-from-top-1">
                  This content is visible. The button above has `aria-expanded="true"`.
                </div>
              )}
            </div>
          </div>

          {/* Case 4: Navigation State */}
          <div className="p-4 border rounded-md space-y-2">
            <h3 className="font-medium text-sm text-muted-foreground">Current Page (requires aria-current)</h3>
            <nav className="flex gap-2">
              <a href="#" className="px-3 py-1 rounded text-sm text-muted-foreground hover:bg-muted">Page 1</a>
              <a href="#" className="px-3 py-1 rounded text-sm bg-primary text-primary-foreground font-medium" aria-current="page">Page 2</a>
              <a href="#" className="px-3 py-1 rounded text-sm text-muted-foreground hover:bg-muted">Page 3</a>
            </nav>
            <span className="text-xs text-muted-foreground">Page 2 has `aria-current="page"`</span>
          </div>
        </div>
      </Section>
    </div>
  );
}
