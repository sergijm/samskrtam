import React from 'react';
import { Avatar } from 'primereact/avatar';

interface UserAvatarProps {
  username: string; // Keep username for initial if firstName/lastName are not available
  firstName?: string; // Added firstName
  lastName?: string;  // Added lastName
  email?: string;
  avatarUrl?: string;
  size?: 'normal' | 'large' | 'xlarge';
  shape?: 'square' | 'circle';
}

const UserAvatar = ({ username, firstName, lastName, email, avatarUrl, size = 'normal', shape = 'circle' }: UserAvatarProps) => {
  const displayName = (firstName && lastName) ? `${firstName} ${lastName}` : username;
  const initial = displayName ? displayName.charAt(0).toUpperCase() : '?';

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
        <div className="font-bold">{displayName}</div> {/* Display firstName + lastName or username */}
        {email && <div className="text-sm text-color-secondary">{email}</div>} {/* Display email */}
      </div>
    </div>
  );
};

export default UserAvatar;
