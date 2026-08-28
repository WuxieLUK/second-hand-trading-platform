import { defineStore } from 'pinia';

export const useLoadingStore = defineStore('loading', {
    state: () => ({
        isLoading: false
    }),
    actions: {
        setLoading(value) {
            this.isLoading = value;
        }
    }
});