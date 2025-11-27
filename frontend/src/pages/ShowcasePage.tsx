import { useState } from 'react';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from '@/components/ui/accordion';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { AspectRatio } from '@/components/ui/aspect-ratio';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbPage,
    BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card';
import {
    Carousel,
    CarouselContent,
    CarouselItem,
    CarouselNext,
    CarouselPrevious,
} from '@/components/ui/carousel';
import { Checkbox } from '@/components/ui/checkbox';
import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from '@/components/ui/collapsible';
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from '@/components/ui/command';
import {
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuTrigger,
} from '@/components/ui/context-menu';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {
    Drawer,
    DrawerClose,
    DrawerContent,
    DrawerDescription,
    DrawerFooter,
    DrawerHeader,
    DrawerTitle,
    DrawerTrigger,
} from '@/components/ui/drawer';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
    HoverCard,
    HoverCardContent,
    HoverCardTrigger,
} from '@/components/ui/hover-card';
import { Input } from '@/components/ui/input';
import {
    InputOTP,
    InputOTPGroup,
    InputOTPSlot,
} from '@/components/ui/input-otp';
import { Label } from '@/components/ui/label';
import {
    Menubar,
    MenubarContent,
    MenubarItem,
    MenubarMenu,
    MenubarSeparator,
    MenubarShortcut,
    MenubarTrigger,
} from '@/components/ui/menubar';
import {
    NavigationMenu,
    NavigationMenuContent,
    NavigationMenuItem,
    NavigationMenuLink,
    NavigationMenuList,
    NavigationMenuTrigger,
    navigationMenuTriggerStyle,
} from '@/components/ui/navigation-menu';
import {
    Pagination,
    PaginationContent,
    PaginationEllipsis,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious,
} from '@/components/ui/pagination';
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/components/ui/popover';
import { Progress } from '@/components/ui/progress';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import {
    ResizableHandle,
    ResizablePanel,
    ResizablePanelGroup,
} from '@/components/ui/resizable';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { Separator } from '@/components/ui/separator';
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
    SheetTrigger,
} from '@/components/ui/sheet';
import { Skeleton } from '@/components/ui/skeleton';
import { Slider } from '@/components/ui/slider';
import { Switch } from '@/components/ui/switch';
import {
    Table,
    TableBody,
    TableCaption,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Textarea } from '@/components/ui/textarea';
import { Toggle } from '@/components/ui/toggle';
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group';
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip';
import {
    ChartContainer,
    ChartTooltip,
    ChartTooltipContent,
    ChartLegend,
    ChartLegendContent,
} from '@/components/ui/chart';
import {
    Sidebar,
    SidebarContent,
    SidebarGroup,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarProvider,
} from '@/components/ui/sidebar';
import { Bar, BarChart, CartesianGrid, XAxis } from 'recharts';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { Toast } from '@/components/feedback/Toast';
import { Check, Copy, Terminal, Bold, Italic, Underline } from 'lucide-react';

interface ShowcaseItemProps {
    title: string;
    description?: string;
    code: string;
    children: React.ReactNode;
}

function ShowcaseItem({ title, description, code, children }: ShowcaseItemProps) {
    const [copied, setCopied] = useState(false);

    const handleCopy = () => {
        navigator.clipboard.writeText(code);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <div className="border rounded-lg overflow-hidden bg-card text-card-foreground shadow-sm">
            <div className="p-6 border-b bg-muted/30">
                <h3 className="text-lg font-semibold mb-2">{title}</h3>
                {description && <p className="text-sm text-muted-foreground mb-4">{description}</p>}
                <div className="flex items-center justify-center p-8 border rounded-md bg-background border-dashed overflow-auto">
                    {children}
                </div>
            </div>
            <div className="relative bg-muted/50 p-4">
                <div className="absolute right-4 top-4">
                    <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8"
                        onClick={handleCopy}
                        title="Copy code"
                    >
                        {copied ? <Check className="h-4 w-4 text-green-500" /> : <Copy className="h-4 w-4" />}
                    </Button>
                </div>
                <pre className="text-sm overflow-x-auto p-4 pt-2 font-mono text-muted-foreground">
                    <code>{code}</code>
                </pre>
            </div>
        </div>
    );
}

export default function ShowcasePage() {
    const [showToast, setShowToast] = useState(false);
    const [date, setDate] = useState<Date | undefined>(new Date());

    return (
        <div className="container mx-auto py-10 space-y-10">
            <div className="space-y-2">
                <h1 className="text-3xl font-bold tracking-tight">UI & Feedback Showcase</h1>
                <p className="text-muted-foreground">
                    A collection of shared components with usage examples.
                </p>
            </div>

            <div className="grid grid-cols-1 gap-10">
                {/* Accordion */}
                <ShowcaseItem
                    title="Accordion"
                    code={`<Accordion type="single" collapsible className="w-full">
  <AccordionItem value="item-1">
    <AccordionTrigger>Is it accessible?</AccordionTrigger>
    <AccordionContent>Yes. It adheres to the WAI-ARIA design pattern.</AccordionContent>
  </AccordionItem>
</Accordion>`}
                >
                    <Accordion type="single" collapsible className="w-full max-w-sm">
                        <AccordionItem value="item-1">
                            <AccordionTrigger>Is it accessible?</AccordionTrigger>
                            <AccordionContent>Yes. It adheres to the WAI-ARIA design pattern.</AccordionContent>
                        </AccordionItem>
                        <AccordionItem value="item-2">
                            <AccordionTrigger>Is it styled?</AccordionTrigger>
                            <AccordionContent>Yes. It comes with default styles that matches the other components' aesthetic.</AccordionContent>
                        </AccordionItem>
                    </Accordion>
                </ShowcaseItem>

                {/* Alert */}
                <ShowcaseItem
                    title="Alert"
                    code={`<Alert>
  <Terminal className="h-4 w-4" />
  <AlertTitle>Heads up!</AlertTitle>
  <AlertDescription>You can add components to your app using the cli.</AlertDescription>
</Alert>`}
                >
                    <div className="w-full max-w-sm space-y-4">
                        <Alert>
                            <Terminal className="h-4 w-4" />
                            <AlertTitle>Heads up!</AlertTitle>
                            <AlertDescription>You can add components to your app using the cli.</AlertDescription>
                        </Alert>
                        <Alert variant="destructive">
                            <Terminal className="h-4 w-4" />
                            <AlertTitle>Error</AlertTitle>
                            <AlertDescription>Your session has expired. Please log in again.</AlertDescription>
                        </Alert>
                    </div>
                </ShowcaseItem>

                {/* AlertDialog */}
                <ShowcaseItem
                    title="AlertDialog"
                    code={`<AlertDialog>
  <AlertDialogTrigger asChild>
    <Button variant="outline">Show Dialog</Button>
  </AlertDialogTrigger>
  <AlertDialogContent>
    <AlertDialogHeader>
      <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>
      <AlertDialogDescription>
        This action cannot be undone.
      </AlertDialogDescription>
    </AlertDialogHeader>
    <AlertDialogFooter>
      <AlertDialogCancel>Cancel</AlertDialogCancel>
      <AlertDialogAction>Continue</AlertDialogAction>
    </AlertDialogFooter>
  </AlertDialogContent>
</AlertDialog>`}
                >
                    <AlertDialog>
                        <AlertDialogTrigger asChild>
                            <Button variant="outline">Show Dialog</Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                            <AlertDialogHeader>
                                <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>
                                <AlertDialogDescription>
                                    This action cannot be undone. This will permanently delete your account and remove your data from our servers.
                                </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                                <AlertDialogCancel>Cancel</AlertDialogCancel>
                                <AlertDialogAction>Continue</AlertDialogAction>
                            </AlertDialogFooter>
                        </AlertDialogContent>
                    </AlertDialog>
                </ShowcaseItem>

                {/* AspectRatio */}
                <ShowcaseItem
                    title="AspectRatio"
                    code={`<AspectRatio ratio={16 / 9} className="bg-muted">
  <img src="..." alt="Photo" className="rounded-md object-cover" />
</AspectRatio>`}
                >
                    <div className="w-[300px]">
                        <AspectRatio ratio={16 / 9} className="bg-muted flex items-center justify-center rounded-md">
                            <span className="text-muted-foreground">16:9 Aspect Ratio</span>
                        </AspectRatio>
                    </div>
                </ShowcaseItem>

                {/* Avatar */}
                <ShowcaseItem
                    title="Avatar"
                    code={`<Avatar>
  <AvatarImage src="https://github.com/shadcn.png" />
  <AvatarFallback>CN</AvatarFallback>
</Avatar>`}
                >
                    <div className="flex gap-4">
                        <Avatar>
                            <AvatarImage src="https://github.com/shadcn.png" />
                            <AvatarFallback>CN</AvatarFallback>
                        </Avatar>
                        <Avatar>
                            <AvatarFallback>JD</AvatarFallback>
                        </Avatar>
                    </div>
                </ShowcaseItem>

                {/* Badge */}
                <ShowcaseItem
                    title="Badge"
                    code={`<Badge>Badge</Badge>`}
                >
                    <div className="flex gap-2">
                        <Badge>Default</Badge>
                        <Badge variant="secondary">Secondary</Badge>
                        <Badge variant="outline">Outline</Badge>
                        <Badge variant="destructive">Destructive</Badge>
                    </div>
                </ShowcaseItem>

                {/* Breadcrumb */}
                <ShowcaseItem
                    title="Breadcrumb"
                    code={`<Breadcrumb>
  <BreadcrumbList>
    <BreadcrumbItem>
      <BreadcrumbLink href="/">Home</BreadcrumbLink>
    </BreadcrumbItem>
    <BreadcrumbSeparator />
    <BreadcrumbItem>
      <BreadcrumbPage>Breadcrumb</BreadcrumbPage>
    </BreadcrumbItem>
  </BreadcrumbList>
</Breadcrumb>`}
                >
                    <Breadcrumb>
                        <BreadcrumbList>
                            <BreadcrumbItem>
                                <BreadcrumbLink href="#">Home</BreadcrumbLink>
                            </BreadcrumbItem>
                            <BreadcrumbSeparator />
                            <BreadcrumbItem>
                                <BreadcrumbLink href="#">Components</BreadcrumbLink>
                            </BreadcrumbItem>
                            <BreadcrumbSeparator />
                            <BreadcrumbItem>
                                <BreadcrumbPage>Breadcrumb</BreadcrumbPage>
                            </BreadcrumbItem>
                        </BreadcrumbList>
                    </Breadcrumb>
                </ShowcaseItem>

                {/* Button */}
                <ShowcaseItem
                    title="Button"
                    code={`<Button>Button</Button>`}
                >
                    <div className="flex gap-2 flex-wrap">
                        <Button>Default</Button>
                        <Button variant="secondary">Secondary</Button>
                        <Button variant="destructive">Destructive</Button>
                        <Button variant="outline">Outline</Button>
                        <Button variant="ghost">Ghost</Button>
                        <Button variant="link">Link</Button>
                    </div>
                </ShowcaseItem>

                {/* Calendar */}
                <ShowcaseItem
                    title="Calendar"
                    code={`<Calendar
  mode="single"
  selected={date}
  onSelect={setDate}
  className="rounded-md border"
/>`}
                >
                    <Calendar
                        mode="single"
                        selected={date}
                        onSelect={setDate}
                        className="rounded-md border"
                    />
                </ShowcaseItem>

                {/* Card */}
                <ShowcaseItem
                    title="Card"
                    code={`<Card>
  <CardHeader>
    <CardTitle>Card Title</CardTitle>
    <CardDescription>Card Description</CardDescription>
  </CardHeader>
  <CardContent>
    <p>Card Content</p>
  </CardContent>
  <CardFooter>
    <p>Card Footer</p>
  </CardFooter>
</Card>`}
                >
                    <Card className="w-[350px]">
                        <CardHeader>
                            <CardTitle>Card Title</CardTitle>
                            <CardDescription>Card Description</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <p>Card Content</p>
                        </CardContent>
                        <CardFooter>
                            <p>Card Footer</p>
                        </CardFooter>
                    </Card>
                </ShowcaseItem>

                {/* Carousel */}
                <ShowcaseItem
                    title="Carousel"
                    code={`<Carousel className="w-full max-w-xs">
  <CarouselContent>
    <CarouselItem>...</CarouselItem>
  </CarouselContent>
  <CarouselPrevious />
  <CarouselNext />
</Carousel>`}
                >
                    <Carousel className="w-full max-w-xs">
                        <CarouselContent>
                            {Array.from({ length: 5 }).map((_, index) => (
                                <CarouselItem key={index}>
                                    <div className="p-1">
                                        <Card>
                                            <CardContent className="flex aspect-square items-center justify-center p-6">
                                                <span className="text-4xl font-semibold">{index + 1}</span>
                                            </CardContent>
                                        </Card>
                                    </div>
                                </CarouselItem>
                            ))}
                        </CarouselContent>
                        <CarouselPrevious />
                        <CarouselNext />
                    </Carousel>
                </ShowcaseItem>

                {/* Checkbox */}
                <ShowcaseItem
                    title="Checkbox"
                    code={`<div className="flex items-center space-x-2">
  <Checkbox id="terms" />
  <Label htmlFor="terms">Accept terms and conditions</Label>
</div>`}
                >
                    <div className="flex items-center space-x-2">
                        <Checkbox id="terms" />
                        <Label htmlFor="terms">Accept terms and conditions</Label>
                    </div>
                </ShowcaseItem>

                {/* Collapsible */}
                <ShowcaseItem
                    title="Collapsible"
                    code={`<Collapsible>
  <CollapsibleTrigger>Can I use this in my project?</CollapsibleTrigger>
  <CollapsibleContent>
    Yes. Free to use for personal and commercial projects.
  </CollapsibleContent>
</Collapsible>`}
                >
                    <Collapsible className="w-[350px] space-y-2">
                        <div className="flex items-center justify-between space-x-4 px-4">
                            <h4 className="text-sm font-semibold">
                                @peduarte starred 3 repositories
                            </h4>
                            <CollapsibleTrigger asChild>
                                <Button variant="ghost" size="sm" className="w-9 p-0">
                                    <span className="sr-only">Toggle</span>
                                    <div className="h-4 w-4 border border-current" />
                                </Button>
                            </CollapsibleTrigger>
                        </div>
                        <div className="rounded-md border px-4 py-3 font-mono text-sm">
                            @radix-ui/primitives
                        </div>
                        <CollapsibleContent className="space-y-2">
                            <div className="rounded-md border px-4 py-3 font-mono text-sm">
                                @radix-ui/colors
                            </div>
                            <div className="rounded-md border px-4 py-3 font-mono text-sm">
                                @stitches/react
                            </div>
                        </CollapsibleContent>
                    </Collapsible>
                </ShowcaseItem>

                {/* Command */}
                <ShowcaseItem
                    title="Command"
                    code={`<Command>
  <CommandInput placeholder="Type a command or search..." />
  <CommandList>
    <CommandEmpty>No results found.</CommandEmpty>
    <CommandGroup heading="Suggestions">
      <CommandItem>Calendar</CommandItem>
      <CommandItem>Search Emoji</CommandItem>
      <CommandItem>Calculator</CommandItem>
    </CommandGroup>
  </CommandList>
</Command>`}
                >
                    <Command className="rounded-lg border shadow-md w-[450px]">
                        <CommandInput placeholder="Type a command or search..." />
                        <CommandList>
                            <CommandEmpty>No results found.</CommandEmpty>
                            <CommandGroup heading="Suggestions">
                                <CommandItem>Calendar</CommandItem>
                                <CommandItem>Search Emoji</CommandItem>
                                <CommandItem>Calculator</CommandItem>
                            </CommandGroup>
                        </CommandList>
                    </Command>
                </ShowcaseItem>

                {/* ContextMenu */}
                <ShowcaseItem
                    title="ContextMenu"
                    code={`<ContextMenu>
  <ContextMenuTrigger>Right click here</ContextMenuTrigger>
  <ContextMenuContent>
    <ContextMenuItem>Profile</ContextMenuItem>
    <ContextMenuItem>Billing</ContextMenuItem>
    <ContextMenuItem>Team</ContextMenuItem>
    <ContextMenuItem>Subscription</ContextMenuItem>
  </ContextMenuContent>
</ContextMenu>`}
                >
                    <ContextMenu>
                        <ContextMenuTrigger className="flex h-[150px] w-[300px] items-center justify-center rounded-md border border-dashed text-sm">
                            Right click here
                        </ContextMenuTrigger>
                        <ContextMenuContent className="w-64">
                            <ContextMenuItem inset>Back</ContextMenuItem>
                            <ContextMenuItem inset disabled>
                                Forward
                            </ContextMenuItem>
                            <ContextMenuItem inset>Reload</ContextMenuItem>
                            <ContextMenuItem inset>Save As...</ContextMenuItem>
                            <ContextMenuItem inset>Print...</ContextMenuItem>
                        </ContextMenuContent>
                    </ContextMenu>
                </ShowcaseItem>

                {/* Dialog */}
                <ShowcaseItem
                    title="Dialog"
                    code={`<Dialog>
  <DialogTrigger>Open</DialogTrigger>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>Are you sure?</DialogTitle>
      <DialogDescription>
        This action cannot be undone.
      </DialogDescription>
    </DialogHeader>
  </DialogContent>
</Dialog>`}
                >
                    <Dialog>
                        <DialogTrigger asChild>
                            <Button variant="outline">Open Dialog</Button>
                        </DialogTrigger>
                        <DialogContent>
                            <DialogHeader>
                                <DialogTitle>Edit profile</DialogTitle>
                                <DialogDescription>
                                    Make changes to your profile here. Click save when you're done.
                                </DialogDescription>
                            </DialogHeader>
                            <div className="grid gap-4 py-4">
                                <div className="grid grid-cols-4 items-center gap-4">
                                    <Label htmlFor="name" className="text-right">
                                        Name
                                    </Label>
                                    <Input id="name" value="Pedro Duarte" className="col-span-3" />
                                </div>
                            </div>
                            <DialogFooter>
                                <Button type="submit">Save changes</Button>
                            </DialogFooter>
                        </DialogContent>
                    </Dialog>
                </ShowcaseItem>

                {/* Drawer */}
                <ShowcaseItem
                    title="Drawer"
                    code={`<Drawer>
  <DrawerTrigger>Open</DrawerTrigger>
  <DrawerContent>
    <DrawerHeader>
      <DrawerTitle>Are you sure?</DrawerTitle>
      <DrawerDescription>This action cannot be undone.</DrawerDescription>
    </DrawerHeader>
    <DrawerFooter>
      <Button>Submit</Button>
      <DrawerClose>
        <Button variant="outline">Cancel</Button>
      </DrawerClose>
    </DrawerFooter>
  </DrawerContent>
</Drawer>`}
                >
                    <Drawer>
                        <DrawerTrigger asChild>
                            <Button variant="outline">Open Drawer</Button>
                        </DrawerTrigger>
                        <DrawerContent>
                            <div className="mx-auto w-full max-w-sm">
                                <DrawerHeader>
                                    <DrawerTitle>Move Goal</DrawerTitle>
                                    <DrawerDescription>Set your daily activity goal.</DrawerDescription>
                                </DrawerHeader>
                                <div className="p-4 pb-0">
                                    <div className="flex items-center justify-center space-x-2">
                                        <span className="text-4xl font-bold tracking-tighter">
                                            350
                                        </span>
                                        <span className="text-[0.70rem] uppercase text-muted-foreground">
                                            Calories/day
                                        </span>
                                    </div>
                                </div>
                                <DrawerFooter>
                                    <Button>Submit</Button>
                                    <DrawerClose asChild>
                                        <Button variant="outline">Cancel</Button>
                                    </DrawerClose>
                                </DrawerFooter>
                            </div>
                        </DrawerContent>
                    </Drawer>
                </ShowcaseItem>

                {/* DropdownMenu */}
                <ShowcaseItem
                    title="DropdownMenu"
                    code={`<DropdownMenu>
  <DropdownMenuTrigger>Open</DropdownMenuTrigger>
  <DropdownMenuContent>
    <DropdownMenuLabel>My Account</DropdownMenuLabel>
    <DropdownMenuSeparator />
    <DropdownMenuItem>Profile</DropdownMenuItem>
    <DropdownMenuItem>Billing</DropdownMenuItem>
    <DropdownMenuItem>Team</DropdownMenuItem>
    <DropdownMenuItem>Subscription</DropdownMenuItem>
  </DropdownMenuContent>
</DropdownMenu>`}
                >
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="outline">Open Menu</Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent>
                            <DropdownMenuLabel>My Account</DropdownMenuLabel>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem>Profile</DropdownMenuItem>
                            <DropdownMenuItem>Billing</DropdownMenuItem>
                            <DropdownMenuItem>Team</DropdownMenuItem>
                            <DropdownMenuItem>Subscription</DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </ShowcaseItem>

                {/* HoverCard */}
                <ShowcaseItem
                    title="HoverCard"
                    code={`<HoverCard>
  <HoverCardTrigger>Hover</HoverCardTrigger>
  <HoverCardContent>
    The React Framework – created and maintained by @vercel.
  </HoverCardContent>
</HoverCard>`}
                >
                    <HoverCard>
                        <HoverCardTrigger asChild>
                            <Button variant="link">@nextjs</Button>
                        </HoverCardTrigger>
                        <HoverCardContent className="w-80">
                            <div className="flex justify-between space-x-4">
                                <Avatar>
                                    <AvatarImage src="https://github.com/vercel.png" />
                                    <AvatarFallback>VC</AvatarFallback>
                                </Avatar>
                                <div className="space-y-1">
                                    <h4 className="text-sm font-semibold">@nextjs</h4>
                                    <p className="text-sm">
                                        The React Framework – created and maintained by @vercel.
                                    </p>
                                    <div className="flex items-center pt-2">
                                        <span className="text-xs text-muted-foreground">
                                            Joined December 2021
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </HoverCardContent>
                    </HoverCard>
                </ShowcaseItem>

                {/* Input */}
                <ShowcaseItem
                    title="Input"
                    code={`<Input type="email" placeholder="Email" />`}
                >
                    <div className="w-full max-w-sm">
                        <Input type="email" placeholder="Email" />
                    </div>
                </ShowcaseItem>

                {/* InputOTP */}
                <ShowcaseItem
                    title="InputOTP"
                    code={`<InputOTP maxLength={6}>
  <InputOTPGroup>
    <InputOTPSlot index={0} />
    <InputOTPSlot index={1} />
    <InputOTPSlot index={2} />
  </InputOTPGroup>
  <InputOTPGroup>
    <InputOTPSlot index={3} />
    <InputOTPSlot index={4} />
    <InputOTPSlot index={5} />
  </InputOTPGroup>
</InputOTP>`}
                >
                    <InputOTP maxLength={6}>
                        <InputOTPGroup>
                            <InputOTPSlot index={0} />
                            <InputOTPSlot index={1} />
                            <InputOTPSlot index={2} />
                        </InputOTPGroup>
                        <InputOTPGroup>
                            <InputOTPSlot index={3} />
                            <InputOTPSlot index={4} />
                            <InputOTPSlot index={5} />
                        </InputOTPGroup>
                    </InputOTP>
                </ShowcaseItem>

                {/* Label */}
                <ShowcaseItem
                    title="Label"
                    code={`<Label htmlFor="email">Your email address</Label>`}
                >
                    <div className="flex items-center space-x-2">
                        <Checkbox id="terms-2" />
                        <Label htmlFor="terms-2">Accept terms and conditions</Label>
                    </div>
                </ShowcaseItem>

                {/* Menubar */}
                <ShowcaseItem
                    title="Menubar"
                    code={`<Menubar>
  <MenubarMenu>
    <MenubarTrigger>File</MenubarTrigger>
    <MenubarContent>
      <MenubarItem>New Tab <MenubarShortcut>⌘T</MenubarShortcut></MenubarItem>
      <MenubarItem>New Window</MenubarItem>
      <MenubarSeparator />
      <MenubarItem>Share</MenubarItem>
      <MenubarSeparator />
      <MenubarItem>Print</MenubarItem>
    </MenubarContent>
  </MenubarMenu>
</Menubar>`}
                >
                    <Menubar>
                        <MenubarMenu>
                            <MenubarTrigger>File</MenubarTrigger>
                            <MenubarContent>
                                <MenubarItem>
                                    New Tab <MenubarShortcut>⌘T</MenubarShortcut>
                                </MenubarItem>
                                <MenubarItem>New Window</MenubarItem>
                                <MenubarSeparator />
                                <MenubarItem>Share</MenubarItem>
                                <MenubarSeparator />
                                <MenubarItem>Print</MenubarItem>
                            </MenubarContent>
                        </MenubarMenu>
                    </Menubar>
                </ShowcaseItem>

                {/* NavigationMenu */}
                <ShowcaseItem
                    title="NavigationMenu"
                    code={`<NavigationMenu>
  <NavigationMenuList>
    <NavigationMenuItem>
      <NavigationMenuTrigger>Item One</NavigationMenuTrigger>
      <NavigationMenuContent>
        <NavigationMenuLink>Link</NavigationMenuLink>
      </NavigationMenuContent>
    </NavigationMenuItem>
  </NavigationMenuList>
</NavigationMenu>`}
                >
                    <NavigationMenu>
                        <NavigationMenuList>
                            <NavigationMenuItem>
                                <NavigationMenuTrigger>Item One</NavigationMenuTrigger>
                                <NavigationMenuContent>
                                    <ul className="grid gap-3 p-6 md:w-[400px] lg:w-[500px] lg:grid-cols-[.75fr_1fr]">
                                        <li className="row-span-3">
                                            <NavigationMenuLink asChild>
                                                <a
                                                    className="flex h-full w-full select-none flex-col justify-end rounded-md bg-gradient-to-b from-muted/50 to-muted p-6 no-underline outline-none focus:shadow-md"
                                                    href="/"
                                                >
                                                    <div className="mb-2 mt-4 text-lg font-medium">
                                                        shadcn/ui
                                                    </div>
                                                    <p className="text-sm leading-tight text-muted-foreground">
                                                        Beautifully designed components built with Radix UI and
                                                        Tailwind CSS.
                                                    </p>
                                                </a>
                                            </NavigationMenuLink>
                                        </li>
                                        <li className="col-span-1">
                                            <NavigationMenuLink className={navigationMenuTriggerStyle()}>
                                                Documentation
                                            </NavigationMenuLink>
                                        </li>
                                    </ul>
                                </NavigationMenuContent>
                            </NavigationMenuItem>
                        </NavigationMenuList>
                    </NavigationMenu>
                </ShowcaseItem>

                {/* Pagination */}
                <ShowcaseItem
                    title="Pagination"
                    code={`<Pagination>
  <PaginationContent>
    <PaginationItem>
      <PaginationPrevious href="#" />
    </PaginationItem>
    <PaginationItem>
      <PaginationLink href="#">1</PaginationLink>
    </PaginationItem>
    <PaginationItem>
      <PaginationEllipsis />
    </PaginationItem>
    <PaginationItem>
      <PaginationNext href="#" />
    </PaginationItem>
  </PaginationContent>
</Pagination>`}
                >
                    <Pagination>
                        <PaginationContent>
                            <PaginationItem>
                                <PaginationPrevious href="#" />
                            </PaginationItem>
                            <PaginationItem>
                                <PaginationLink href="#">1</PaginationLink>
                            </PaginationItem>
                            <PaginationItem>
                                <PaginationLink href="#" isActive>
                                    2
                                </PaginationLink>
                            </PaginationItem>
                            <PaginationItem>
                                <PaginationLink href="#">3</PaginationLink>
                            </PaginationItem>
                            <PaginationItem>
                                <PaginationEllipsis />
                            </PaginationItem>
                            <PaginationItem>
                                <PaginationNext href="#" />
                            </PaginationItem>
                        </PaginationContent>
                    </Pagination>
                </ShowcaseItem>

                {/* Popover */}
                <ShowcaseItem
                    title="Popover"
                    code={`<Popover>
  <PopoverTrigger>Open</PopoverTrigger>
  <PopoverContent>Place content for the popover here.</PopoverContent>
</Popover>`}
                >
                    <Popover>
                        <PopoverTrigger asChild>
                            <Button variant="outline">Open Popover</Button>
                        </PopoverTrigger>
                        <PopoverContent className="w-80">
                            <div className="grid gap-4">
                                <div className="space-y-2">
                                    <h4 className="font-medium leading-none">Dimensions</h4>
                                    <p className="text-sm text-muted-foreground">
                                        Set the dimensions for the layer.
                                    </p>
                                </div>
                            </div>
                        </PopoverContent>
                    </Popover>
                </ShowcaseItem>

                {/* Progress */}
                <ShowcaseItem
                    title="Progress"
                    code={`<Progress value={33} />`}
                >
                    <div className="w-[300px]">
                        <Progress value={33} />
                    </div>
                </ShowcaseItem>

                {/* RadioGroup */}
                <ShowcaseItem
                    title="RadioGroup"
                    code={`<RadioGroup defaultValue="option-one">
  <div className="flex items-center space-x-2">
    <RadioGroupItem value="option-one" id="option-one" />
    <Label htmlFor="option-one">Option One</Label>
  </div>
  <div className="flex items-center space-x-2">
    <RadioGroupItem value="option-two" id="option-two" />
    <Label htmlFor="option-two">Option Two</Label>
  </div>
</RadioGroup>`}
                >
                    <RadioGroup defaultValue="option-one">
                        <div className="flex items-center space-x-2">
                            <RadioGroupItem value="option-one" id="option-one" />
                            <Label htmlFor="option-one">Option One</Label>
                        </div>
                        <div className="flex items-center space-x-2">
                            <RadioGroupItem value="option-two" id="option-two" />
                            <Label htmlFor="option-two">Option Two</Label>
                        </div>
                    </RadioGroup>
                </ShowcaseItem>

                {/* Resizable */}
                <ShowcaseItem
                    title="Resizable"
                    code={`<ResizablePanelGroup direction="horizontal">
  <ResizablePanel>One</ResizablePanel>
  <ResizableHandle />
  <ResizablePanel>Two</ResizablePanel>
</ResizablePanelGroup>`}
                >
                    <ResizablePanelGroup
                        direction="horizontal"
                        className="max-w-md rounded-lg border"
                    >
                        <ResizablePanel defaultSize={50}>
                            <div className="flex h-[200px] items-center justify-center p-6">
                                <span className="font-semibold">One</span>
                            </div>
                        </ResizablePanel>
                        <ResizableHandle />
                        <ResizablePanel defaultSize={50}>
                            <ResizablePanelGroup direction="vertical">
                                <ResizablePanel defaultSize={25}>
                                    <div className="flex h-full items-center justify-center p-6">
                                        <span className="font-semibold">Two</span>
                                    </div>
                                </ResizablePanel>
                                <ResizableHandle />
                                <ResizablePanel defaultSize={75}>
                                    <div className="flex h-full items-center justify-center p-6">
                                        <span className="font-semibold">Three</span>
                                    </div>
                                </ResizablePanel>
                            </ResizablePanelGroup>
                        </ResizablePanel>
                    </ResizablePanelGroup>
                </ShowcaseItem>

                {/* ScrollArea */}
                <ShowcaseItem
                    title="ScrollArea"
                    code={`<ScrollArea className="h-[200px] w-[350px] rounded-md border p-4">
  Jokester began sneaking into the castle in the middle of the night and leaving
  jokes all over the place: under the king's pillow, in his soup, even in the
  royal toilet. The king was furious, but he couldn't seem to stop Jokester. And
  then, one day, the people of the kingdom discovered that the jokes were
  actually funny, and they started laughing. And then the king started laughing,
  and soon everyone was laughing.
</ScrollArea>`}
                >
                    <ScrollArea className="h-[200px] w-[350px] rounded-md border p-4">
                        Jokester began sneaking into the castle in the middle of the night and leaving
                        jokes all over the place: under the king's pillow, in his soup, even in the
                        royal toilet. The king was furious, but he couldn't seem to stop Jokester. And
                        then, one day, the people of the kingdom discovered that the jokes were
                        actually funny, and they started laughing. And then the king started laughing,
                        and soon everyone was laughing.
                    </ScrollArea>
                </ShowcaseItem>

                {/* Select */}
                <ShowcaseItem
                    title="Select"
                    code={`<Select>
  <SelectTrigger className="w-[180px]">
    <SelectValue placeholder="Theme" />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="light">Light</SelectItem>
    <SelectItem value="dark">Dark</SelectItem>
    <SelectItem value="system">System</SelectItem>
  </SelectContent>
</Select>`}
                >
                    <Select>
                        <SelectTrigger className="w-[180px]">
                            <SelectValue placeholder="Theme" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="light">Light</SelectItem>
                            <SelectItem value="dark">Dark</SelectItem>
                            <SelectItem value="system">System</SelectItem>
                        </SelectContent>
                    </Select>
                </ShowcaseItem>

                {/* Separator */}
                <ShowcaseItem
                    title="Separator"
                    code={`<div>
  <div className="space-y-1">
    <h4 className="text-sm font-medium leading-none">Radix Primitives</h4>
    <p className="text-sm text-muted-foreground">
      An open-source UI component library.
    </p>
  </div>
  <Separator className="my-4" />
  <div className="flex h-5 items-center space-x-4 text-sm">
    <div>Blog</div>
    <Separator orientation="vertical" />
    <div>Docs</div>
    <Separator orientation="vertical" />
    <div>Source</div>
  </div>
</div>`}
                >
                    <div>
                        <div className="space-y-1">
                            <h4 className="text-sm font-medium leading-none">Radix Primitives</h4>
                            <p className="text-sm text-muted-foreground">
                                An open-source UI component library.
                            </p>
                        </div>
                        <Separator className="my-4" />
                        <div className="flex h-5 items-center space-x-4 text-sm">
                            <div>Blog</div>
                            <Separator orientation="vertical" />
                            <div>Docs</div>
                            <Separator orientation="vertical" />
                            <div>Source</div>
                        </div>
                    </div>
                </ShowcaseItem>

                {/* Sheet */}
                <ShowcaseItem
                    title="Sheet"
                    code={`<Sheet>
  <SheetTrigger>Open</SheetTrigger>
  <SheetContent>
    <SheetHeader>
      <SheetTitle>Are you sure?</SheetTitle>
      <SheetDescription>
        This action cannot be undone.
      </SheetDescription>
    </SheetHeader>
  </SheetContent>
</Sheet>`}
                >
                    <Sheet>
                        <SheetTrigger asChild>
                            <Button variant="outline">Open Sheet</Button>
                        </SheetTrigger>
                        <SheetContent>
                            <SheetHeader>
                                <SheetTitle>Edit profile</SheetTitle>
                                <SheetDescription>
                                    Make changes to your profile here. Click save when you're done.
                                </SheetDescription>
                            </SheetHeader>
                            <div className="grid gap-4 py-4">
                                <div className="grid grid-cols-4 items-center gap-4">
                                    <Label htmlFor="name" className="text-right">
                                        Name
                                    </Label>
                                    <Input id="name" value="Pedro Duarte" className="col-span-3" />
                                </div>
                            </div>
                            <div className="flex justify-end">
                                <Button type="submit">Save changes</Button>
                            </div>
                        </SheetContent>
                    </Sheet>
                </ShowcaseItem>

                {/* Skeleton */}
                <ShowcaseItem
                    title="Skeleton"
                    code={`<div className="flex items-center space-x-4">
  <Skeleton className="h-12 w-12 rounded-full" />
  <div className="space-y-2">
    <Skeleton className="h-4 w-[250px]" />
    <Skeleton className="h-4 w-[200px]" />
  </div>
</div>`}
                >
                    <div className="flex items-center space-x-4">
                        <Skeleton className="h-12 w-12 rounded-full" />
                        <div className="space-y-2">
                            <Skeleton className="h-4 w-[250px]" />
                            <Skeleton className="h-4 w-[200px]" />
                        </div>
                    </div>
                </ShowcaseItem>

                {/* Slider */}
                <ShowcaseItem
                    title="Slider"
                    code={`<Slider defaultValue={[33]} max={100} step={1} />`}
                >
                    <div className="w-[300px]">
                        <Slider defaultValue={[33]} max={100} step={1} />
                    </div>
                </ShowcaseItem>

                {/* Switch */}
                <ShowcaseItem
                    title="Switch"
                    code={`<div className="flex items-center space-x-2">
  <Switch id="airplane-mode" />
  <Label htmlFor="airplane-mode">Airplane Mode</Label>
</div>`}
                >
                    <div className="flex items-center space-x-2">
                        <Switch id="airplane-mode" />
                        <Label htmlFor="airplane-mode">Airplane Mode</Label>
                    </div>
                </ShowcaseItem>

                {/* Table */}
                <ShowcaseItem
                    title="Table"
                    code={`<Table>
  <TableCaption>A list of your recent invoices.</TableCaption>
  <TableHeader>
    <TableRow>
      <TableHead className="w-[100px]">Invoice</TableHead>
      <TableHead>Status</TableHead>
      <TableHead>Method</TableHead>
      <TableHead className="text-right">Amount</TableHead>
    </TableRow>
  </TableHeader>
  <TableBody>
    <TableRow>
      <TableCell className="font-medium">INV001</TableCell>
      <TableCell>Paid</TableCell>
      <TableCell>Credit Card</TableCell>
      <TableCell className="text-right">$250.00</TableCell>
    </TableRow>
  </TableBody>
</Table>`}
                >
                    <Table>
                        <TableCaption>A list of your recent invoices.</TableCaption>
                        <TableHeader>
                            <TableRow>
                                <TableHead className="w-[100px]">Invoice</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead>Method</TableHead>
                                <TableHead className="text-right">Amount</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            <TableRow>
                                <TableCell className="font-medium">INV001</TableCell>
                                <TableCell>Paid</TableCell>
                                <TableCell>Credit Card</TableCell>
                                <TableCell className="text-right">$250.00</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell className="font-medium">INV002</TableCell>
                                <TableCell>Pending</TableCell>
                                <TableCell>PayPal</TableCell>
                                <TableCell className="text-right">$150.00</TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </ShowcaseItem>

                {/* Tabs */}
                <ShowcaseItem
                    title="Tabs"
                    code={`<Tabs defaultValue="account" className="w-[400px]">
  <TabsList>
    <TabsTrigger value="account">Account</TabsTrigger>
    <TabsTrigger value="password">Password</TabsTrigger>
  </TabsList>
  <TabsContent value="account">Make changes to your account here.</TabsContent>
  <TabsContent value="password">Change your password here.</TabsContent>
</Tabs>`}
                >
                    <Tabs defaultValue="account" className="w-[400px]">
                        <TabsList>
                            <TabsTrigger value="account">Account</TabsTrigger>
                            <TabsTrigger value="password">Password</TabsTrigger>
                        </TabsList>
                        <TabsContent value="account">Make changes to your account here.</TabsContent>
                        <TabsContent value="password">Change your password here.</TabsContent>
                    </Tabs>
                </ShowcaseItem>

                {/* Textarea */}
                <ShowcaseItem
                    title="Textarea"
                    code={`<Textarea placeholder="Type your message here." />`}
                >
                    <div className="w-full max-w-sm">
                        <Textarea placeholder="Type your message here." />
                    </div>
                </ShowcaseItem>

                {/* Toggle */}
                <ShowcaseItem
                    title="Toggle"
                    code={`<Toggle aria-label="Toggle italic">
  <Bold className="h-4 w-4" />
</Toggle>`}
                >
                    <Toggle aria-label="Toggle italic">
                        <Bold className="h-4 w-4" />
                    </Toggle>
                </ShowcaseItem>

                {/* ToggleGroup */}
                <ShowcaseItem
                    title="ToggleGroup"
                    code={`<ToggleGroup type="multiple">
  <ToggleGroupItem value="bold" aria-label="Toggle bold">
    <Bold className="h-4 w-4" />
  </ToggleGroupItem>
  <ToggleGroupItem value="italic" aria-label="Toggle italic">
    <Italic className="h-4 w-4" />
  </ToggleGroupItem>
  <ToggleGroupItem value="underline" aria-label="Toggle underline">
    <Underline className="h-4 w-4" />
  </ToggleGroupItem>
</ToggleGroup>`}
                >
                    <ToggleGroup type="multiple">
                        <ToggleGroupItem value="bold" aria-label="Toggle bold">
                            <Bold className="h-4 w-4" />
                        </ToggleGroupItem>
                        <ToggleGroupItem value="italic" aria-label="Toggle italic">
                            <Italic className="h-4 w-4" />
                        </ToggleGroupItem>
                        <ToggleGroupItem value="underline" aria-label="Toggle underline">
                            <Underline className="h-4 w-4" />
                        </ToggleGroupItem>
                    </ToggleGroup>
                </ShowcaseItem>

                {/* Tooltip */}
                <ShowcaseItem
                    title="Tooltip"
                    code={`<TooltipProvider>
  <Tooltip>
    <TooltipTrigger>Hover</TooltipTrigger>
    <TooltipContent>
      <p>Add to library</p>
    </TooltipContent>
  </Tooltip>
</TooltipProvider>`}
                >
                    <TooltipProvider>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button variant="outline">Hover Me</Button>
                            </TooltipTrigger>
                            <TooltipContent>
                                <p>Add to library</p>
                            </TooltipContent>
                        </Tooltip>
                    </TooltipProvider>
                </ShowcaseItem>

                {/* Chart */}
                <ShowcaseItem
                    title="Chart"
                    code={`<ChartContainer config={chartConfig} className="min-h-[200px] w-full">
  <BarChart accessibilityLayer data={chartData}>
    <CartesianGrid vertical={false} />
    <XAxis
      dataKey="month"
      tickLine={false}
      tickMargin={10}
      axisLine={false}
      tickFormatter={(value) => value.slice(0, 3)}
    />
    <ChartTooltip content={<ChartTooltipContent />} />
    <ChartLegend content={<ChartLegendContent />} />
    <Bar dataKey="desktop" fill="var(--color-desktop)" radius={4} />
    <Bar dataKey="mobile" fill="var(--color-mobile)" radius={4} />
  </BarChart>
</ChartContainer>`}
                >
                    <ChartContainer
                        config={{
                            desktop: {
                                label: "Desktop",
                                color: "hsl(var(--chart-1))",
                            },
                            mobile: {
                                label: "Mobile",
                                color: "hsl(var(--chart-2))",
                            },
                        }}
                        className="min-h-[200px] w-full"
                    >
                        <BarChart accessibilityLayer data={[
                            { month: "January", desktop: 186, mobile: 80 },
                            { month: "February", desktop: 305, mobile: 200 },
                            { month: "March", desktop: 237, mobile: 120 },
                            { month: "April", desktop: 73, mobile: 190 },
                            { month: "May", desktop: 209, mobile: 130 },
                            { month: "June", desktop: 214, mobile: 140 },
                        ]}>
                            <CartesianGrid vertical={false} />
                            <XAxis
                                dataKey="month"
                                tickLine={false}
                                tickMargin={10}
                                axisLine={false}
                                tickFormatter={(value) => value.slice(0, 3)}
                            />
                            <ChartTooltip content={<ChartTooltipContent />} />
                            <ChartLegend content={<ChartLegendContent />} />
                            <Bar dataKey="desktop" fill="var(--color-desktop)" radius={4} />
                            <Bar dataKey="mobile" fill="var(--color-mobile)" radius={4} />
                        </BarChart>
                    </ChartContainer>
                </ShowcaseItem>

                {/* Sidebar */}
                <ShowcaseItem
                    title="Sidebar"
                    code={`<SidebarProvider>
  <Sidebar>
    <SidebarContent>
      <SidebarGroup>
        <SidebarGroupLabel>Application</SidebarGroupLabel>
        <SidebarGroupContent>
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton>
                <span>Home</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroupContent>
      </SidebarGroup>
    </SidebarContent>
  </Sidebar>
</SidebarProvider>`}
                >
                    <div className="h-[300px] w-full border rounded-md overflow-hidden relative">
                        <SidebarProvider className="min-h-[300px]">
                            <Sidebar>
                                <SidebarContent>
                                    <SidebarGroup>
                                        <SidebarGroupLabel>Application</SidebarGroupLabel>
                                        <SidebarGroupContent>
                                            <SidebarMenu>
                                                <SidebarMenuItem>
                                                    <SidebarMenuButton>
                                                        <span>Home</span>
                                                    </SidebarMenuButton>
                                                </SidebarMenuItem>
                                                <SidebarMenuItem>
                                                    <SidebarMenuButton>
                                                        <span>Inbox</span>
                                                    </SidebarMenuButton>
                                                </SidebarMenuItem>
                                            </SidebarMenu>
                                        </SidebarGroupContent>
                                    </SidebarGroup>
                                </SidebarContent>
                            </Sidebar>
                        </SidebarProvider>
                    </div>
                </ShowcaseItem>

                {/* Feedback Components */}
                <ShowcaseItem
                    title="Loading Spinner"
                    description="Indicates a loading state."
                    code={`<LoadingSpinner />
<LoadingSpinner size="sm" />
<LoadingSpinner size="lg" />
<LoadingSpinner message="Loading data..." />`}
                >
                    <div className="flex flex-col gap-4 items-center">
                        <div className="flex gap-4 items-center">
                            <LoadingSpinner size="sm" />
                            <LoadingSpinner />
                            <LoadingSpinner size="lg" />
                        </div>
                        <LoadingSpinner message="Loading data..." />
                    </div>
                </ShowcaseItem>

                <ShowcaseItem
                    title="Toast Notification"
                    description="Displays a temporary notification."
                    code={`{showToast && (
  <Toast
    message="Operation successful!"
    onClose={() => setShowToast(false)}
  />
)}
<Button onClick={() => setShowToast(true)}>Show Toast</Button>`}
                >
                    <div className="relative">
                        <Button onClick={() => setShowToast(true)}>Show Toast</Button>
                        {showToast && (
                            <Toast
                                message="Operation successful!"
                                onClose={() => setShowToast(false)}
                            />
                        )}
                    </div>
                </ShowcaseItem>
            </div>
        </div>
    );
}
