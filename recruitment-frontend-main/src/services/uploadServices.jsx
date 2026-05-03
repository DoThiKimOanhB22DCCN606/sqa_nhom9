import api from "../config/axios";

export const uploadServices = {
  // Upload file lên server
  uploadFile: async (file) => {
    const formData = new FormData();
    formData.append("file", file);

    return await api.post("/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  }
};
