'use client';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { clearToken } from '@/lib/auth';
import { Button } from '@/components/ui/button';

export function NavBar() {
  const pathname = usePathname();
  const router = useRouter();

  function signOut() {
    clearToken();
    router.push('/login');
  }

  return (
    <nav className="border-b bg-background px-6 py-3 flex items-center gap-6">
      <span className="font-semibold text-sm">Roadrunner Admin</span>
      <Link href="/routes" className={`text-sm ${pathname.startsWith('/routes') ? 'font-medium' : 'text-muted-foreground'}`}>
        Routes
      </Link>
      <Link href="/licenses" className={`text-sm ${pathname.startsWith('/licenses') ? 'font-medium' : 'text-muted-foreground'}`}>
        Licenses
      </Link>
      <div className="ml-auto">
        <Button variant="ghost" size="sm" onClick={signOut}>Sign out</Button>
      </div>
    </nav>
  );
}
