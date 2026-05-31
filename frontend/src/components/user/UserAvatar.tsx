import React from 'react';
import { Avatar } from 'primereact/avatar';

interface UserAvatarProps {
  username: string;
  email?: string;
  avatarUrl?: string;
  size?: 'normal' | 'large' | 'xlarge';
  shape?: 'square' | 'circle';
}

const UserAvatar = ({ username, email, avatarUrl, size = 'normal', shape = 'circle' }: UserAvatarProps) => {
  const initial = username ? username.charAt(0).toUpperCase() : '?';

  return (
    <div className="flex align-items-center">
      <Avatar
        image={avatarUrl}
        label={!avatarUrl ? initial : undefined}
        size={size}
        shape={shape}
        className="p-mr-2"
      />
      <div>
        <div className="font-bold">{username}</div>
        {email && <div className="text-sm text-color-secondary">{email}</div>}
      </div>
    </div>
  );
};

export default UserAvatar;
