'use client';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { clearToken } from '@/lib/auth';
import { Button } from '@/components/ui/button';
import { Route, FileText, LogOut, BarChart2 } from 'lucide-react';

export function NavBar() {
  const pathname = usePathname();
  const router = useRouter();
  function signOut() { clearToken(); router.push('/login'); }

  const navLinks = [
    { href: '/routes', label: 'Routes', icon: Route },
    { href: '/licenses', label: 'Licenses', icon: FileText },
    { href: '/stats', label: 'Stats', icon: BarChart2 },
  ];

  return (
    <nav className="sticky top-0 z-50 border-b border-border bg-card/95 backdrop-blur">
      <div className="flex h-14 items-center px-6 gap-8">
        {/* Brand */}
        <div className="flex items-center gap-2 mr-4">
          <span className="text-primary text-xl">●</span>
          <div className="leading-none">
            <div className="text-xs font-bold tracking-[0.2em] text-foreground uppercase">Roadrunner</div>
            <div className="text-[10px] tracking-widest text-primary uppercase">Admin</div>
          </div>
        </div>
        {/* Nav links */}
        <div className="flex items-center gap-1">
          {navLinks.map(({ href, label, icon: Icon }) => {
            const active = pathname.startsWith(href);
            return (
              <Link key={href} href={href}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                  active
                    ? 'bg-primary/15 text-primary'
                    : 'text-muted-foreground hover:text-foreground hover:bg-secondary'
                }`}>
                <Icon size={15} />
                {label}
              </Link>
            );
          })}
        </div>
        {/* Right side */}
        <div className="ml-auto flex items-center gap-3">
          <span className="text-xs text-muted-foreground hidden sm:block">Admin Panel</span>
          <Button variant="ghost" size="sm" onClick={signOut}
            className="gap-2 text-muted-foreground hover:text-destructive hover:bg-destructive/10">
            <LogOut size={14} />
            Sign out
          </Button>
        </div>
      </div>
    </nav>
  );
}
