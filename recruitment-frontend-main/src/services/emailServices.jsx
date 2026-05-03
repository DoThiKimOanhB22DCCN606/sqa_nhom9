import api from "../config/axios";

export const emailServices = {
  sendEmail: async (data) => {
    return await api.post("/email-service/mail/send/gmail", data);
  },
  getInbox: async () => {
    return await api.get("/email-service/mail/inbox");
  },
  getSent: async () => {
    return await api.get("/email-service/mail/sent");
  },
};
