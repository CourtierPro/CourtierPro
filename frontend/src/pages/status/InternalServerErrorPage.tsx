import { Link, useNavigate } from 'react-router-dom';
import { AlertTriangle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, CardHeader, CardTitle, CardDescription, CardFooter } from '@/shared/components/ui/card';
import { Button } from '@/shared/components/ui/button';

export function InternalServerErrorPage() {
  const { t } = useTranslation('status');
  const navigate = useNavigate();

  return (
    <div className="flex min-h-[50vh] items-center justify-center px-4 py-8">
      <Card className="max-w-xl w-full">
        <CardHeader className="flex flex-row items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-orange-100">
            <AlertTriangle className="h-5 w-5 text-orange-600" />
          </div>
          <div>
            <CardTitle>{t('serverErrorTitle')}</CardTitle>
            <CardDescription>
              {t('serverErrorDesc')}
            </CardDescription>
          </div>
        </CardHeader>
        <CardFooter className="flex flex-wrap items-center justify-between gap-2">
          <div className="text-xs text-muted-foreground">
            {t('serverErrorFooter')}
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
