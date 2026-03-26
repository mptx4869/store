import { useState, useRef } from 'react';
import { Upload, X, Image as ImageIcon } from 'lucide-react';
import mediaService from '../../services/mediaService';
import { useToast } from '../../context/ToastContext';

function ImageUpload({ onUploadSuccess, currentImageUrl, onRemove }) {
  const toast = useToast();
  const fileInputRef = useRef(null);

  const [isUploading, setIsUploading] = useState(false);
  const [previewUrl, setPreviewUrl] = useState(currentImageUrl);

  const handleFileSelect = async (event) => {
    const file = event.target.files?.[0]; // ← FIX: Bỏ space giữa ?  và . 
    if (!file) return;

    // Show preview immediately
    const localPreview = URL.createObjectURL(file);
    setPreviewUrl(localPreview);

    setIsUploading(true);
    try {
      const result = await mediaService.uploadImage(file);
      
      toast.success(result.message || 'Image uploaded successfully');
      
      // Call parent callback with uploaded URL
      onUploadSuccess?.(result.url);
      
      // Update preview to server URL
      setPreviewUrl(result.url);
    } catch (err) {
      toast.error(err.message || 'Unable to upload image');
      
      // Revert preview on error
      setPreviewUrl(currentImageUrl);
    } finally {
      setIsUploading(false);
      
      // Reset input
      if (fileInputRef. current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleRemove = () => {
    setPreviewUrl(null);
    onRemove?.();
    
    if (fileInputRef.current) {
      fileInputRef.current. value = '';
    }
  };

  const handleClickUpload = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="space-y-4">
      <label className="block text-sm font-medium text-gray-700">
        Book Image
      </label>

      {/* Preview */}
      {previewUrl ?  (
        <div className="relative inline-block">
          <img
            src={previewUrl}
            alt="Preview"
            className="w-48 h-64 object-cover rounded-lg border border-gray-300"
          />
          
          {! isUploading && (
            <button
              type="button"
              onClick={handleRemove}
              className="absolute top-2 right-2 p-1 bg-red-600 text-white rounded-full hover:bg-red-700 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          )}

          {isUploading && (
            <div className="absolute inset-0 bg-black bg-opacity-50 rounded-lg flex items-center justify-center">
              <div className="text-white text-sm">Uploading...</div>
            </div>
          )}
        </div>
      ) : (
        <div
          onClick={handleClickUpload}
          className="w-48 h-64 border-2 border-dashed border-gray-300 rounded-lg flex flex-col items-center justify-center cursor-pointer hover:border-blue-500 hover:bg-blue-50 transition-colors"
        >
          <ImageIcon className="w-12 h-12 text-gray-400 mb-2" />
          <p className="text-sm text-gray-500">Click to select image</p>
          <p className="text-xs text-gray-400 mt-1">Max 5MB</p>
        </div>
      )}

      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
        onChange={handleFileSelect}
        className="hidden"
        disabled={isUploading}
      />

      {/* Upload button (visible when no preview) */}
      {!previewUrl && (
        <button
          type="button"
          onClick={handleClickUpload}
          disabled={isUploading}
          className="btn-secondary inline-flex items-center gap-2"
        >
          <Upload className="w-4 h-4" />
          {isUploading ? 'Uploading...' : 'Select Image'}
        </button>
      )}

      {/* Change button (visible when has preview) */}
      {previewUrl && ! isUploading && (
        <button
          type="button"
          onClick={handleClickUpload}
          className="btn-secondary inline-flex items-center gap-2"
        >
          <Upload className="w-4 h-4" />
          Change Image
        </button>
      )}

      <p className="text-xs text-gray-500">
        Accepted:  JPEG, PNG, GIF, WebP.  Maximum size:  5MB. 
      </p>
    </div>
  );
}

export default ImageUpload;