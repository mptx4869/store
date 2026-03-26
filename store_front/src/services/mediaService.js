import api from './api';

function buildMediaError(error) {
  const { status, backendMessage } = error. customError || {};
  const fallback = 'Unable to upload image.  Please try again later.';

  if (backendMessage) return new Error(backendMessage);

  if (status === 400) return new Error('Invalid file.  Only accept images under 5MB.');
  if (status === 401) return new Error('Please log in to upload image.');
  if (status === 403) return new Error('You do not have permission to upload image.');
  if (status === 413) return new Error('File too large. Maximum size is 5MB.');

  return new Error(fallback);
}

const mediaService = {
  /**
   * Upload image file
   * @param {File} file - Image file from input[type="file"]
   * @returns {Promise<{url: string, message: string}>}
   */
  async uploadImage(file) {
    // Validate file type
    const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
    if (!validTypes.includes(file.type)) {
      throw new Error('Only accept image files (JPEG, PNG, GIF, WebP).');
    }

    // Validate file size (max 5MB)
    const maxSize = 5 * 1024 * 1024; // 5MB in bytes
    if (file.size > maxSize) {
      throw new Error('File too large.  Maximum size is 5MB.');
    }

    try {
      const formData = new FormData();
      formData.append('file', file);

      // POST /media/upload with multipart/form-data
      const response = await api.post('/media/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return {
        url: response.url,
        message: response.message || 'Upload successful',
      };
    } catch (error) {
      throw buildMediaError(error);
    }
  },
};

export default mediaService;