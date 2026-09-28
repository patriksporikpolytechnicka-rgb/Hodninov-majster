import { Component, Input } from '@angular/core';
@Component({
  selector: 'app-icon', standalone: true,
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="paths[name] || paths[\'tool\']"/></svg>',
  styles: [':host{display:inline-flex;width:1.35em;height:1.35em;flex-shrink:0}svg{width:100%;height:100%}']
})
export class IconComponent {
  @Input() name = 'tool';
  readonly paths: Record<string, string> = {
    tool: 'M14 6a5 5 0 0 0-6 6L3 17a2.8 2.8 0 0 0 4 4l5-5a5 5 0 0 0 6-6l-3 3-4-4 3-3Z',
    home: 'm3 10 9-7 9 7M5 9v12h14V9M9 21v-8h6v8',
    leaf: 'M20 3C9 2 3 8 5 15s14 8 15-12ZM5 20l10-10',
    tech: 'M3 4h18v13H3zM8 21h8m-4-4v4',
    box: 'm3 7 9-4 9 4v12l-9 4-9-4V7Zm0 0 9 4 9-4M12 11v12M7 5l10 4',
    search: 'M20 20l-5-5M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z',
    arrow: 'M4 12h16m-6-6 6 6-6 6',
    pin: 'M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0ZM14 10a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z',
    check: 'm5 12 4 4L19 6',
    clock: 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0ZM12 7v5l3 2',
    user: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2'
  };
}

