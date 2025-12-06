import { Link, useNavigate } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, CardHeader, CardTitle, CardDescription, CardFooter } from '@/shared/components/ui/card';
import { Button } from '@/shared/components/ui/button';

export function ForbiddenPage() {
  const { t } = useTranslation('status');
  const navigate = useNavigate();

  return (
    <div className="flex min-h-[50vh] items-center justify-center px-4 py-8">
      <Card className="max-w-xl w-full">
        <CardHeader className="flex flex-row items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-red-100">
            <ShieldAlert className="h-5 w-5 text-red-600" />
          </div>
          <div>
            <CardTitle>{t('forbiddenTitle')}</CardTitle>
            <CardDescription>
              {t('forbiddenDesc')}
            </CardDescription>
          </div>
        </CardHeader>
        <CardFooter className="flex flex-wrap items-center justify-between gap-2">
          <div className="text-xs text-muted-foreground">
            {t('forbiddenFooter')}
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => navigate(-1)}>
              {t('goBack')}
            </Button>
            <Button size="sm" asChild>
              <Link to="/">{t('goHome')}</Link>
            </Button>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}
