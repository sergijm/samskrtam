import React from 'react';
import { Card } from 'primereact/card';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function AdminHomePage() {
  const { t } = useTranslation();

  const adminItems = [
    { title: t('admin.users.title'), description: t('admin.users.description'), icon: 'pi pi-users', link: '/admin/users' },
    { title: t('groups.title'), description: t('admin.groups.description'), icon: 'pi pi-sitemap', link: '/admin/groups' },
    // Add other admin sections here as needed
  ];

  return (
    <div className="flex flex-column align-items-center p-4">
      <h1 className="text-center mb-5">{t('nav.admin')}</h1>
      <div className="grid justify-content-center w-full" style={{ maxWidth: '1600px' }}>
        {adminItems.map((item, index) => (
          <div key={index} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
            <Link to={item.link} className="no-underline h-full flex w-full">
              <Card
                title={item.title}
                subTitle={item.description}
                className="dashboard-card flex flex-column align-items-center justify-content-between text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
              >
                <div className="flex flex-column align-items-center justify-content-center flex-grow-1">
                  <i className={`${item.icon} text-5xl mb-3`} />
                  <p className="text-sm text-color-secondary">{item.description}</p>
                </div>
              </Card>
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
}
